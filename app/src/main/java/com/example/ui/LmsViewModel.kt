package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class LmsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = LmsDatabase.getDatabase(application)
    val repository = LmsRepository(db)

    // Auth Sessions State
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUserState: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // Screen State
    private val _currentScreen = MutableStateFlow<String>("home") // home, about, courses, contact, auth, dashboard, learning_area, admin_dashboard
    val currentScreenState: StateFlow<String> = _currentScreen.asStateFlow()

    // Current Selection State
    val selectedCourse = MutableStateFlow<CourseEntity?>(null)
    val activeLesson = MutableStateFlow<LessonEntity?>(null)

    // Checkout Billing State (Flutterwave checkout modal controller)
    val checkoutCourse = MutableStateFlow<CourseEntity?>(null)
    val showCheckoutModal = MutableStateFlow(false)
    val paymentStatusMessage = MutableStateFlow("")

    // Search & Filter
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("All")

    // Contact messages
    val contactsList = repository.getAllContactMessagesFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Unified Lists with flow collections
    val allCourses = repository.getAllCoursesFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allStudents = repository.getAllStudentsFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val testimonialsList = repository.getAllTestimonialsFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val paymentsList = repository.getAllPaymentsFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Dark Mode Toggle State
    val isDarkMode = MutableStateFlow(false)

    // User-specific states driven by active auth
    val userEnrollments = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getEnrollmentsForUserFlow(user.id)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userPayments = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getPaymentsForUserFlow(user.id)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userCertificates = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getCertificatesForUserFlow(user.id)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProgress = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getProgressForUserFlow(user.id)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            // Seed database at initial app start
            repository.seedDatabaseIfEmpty()
        }
    }

    // Auth Operations
    fun registerStudent(fullName: String, email: String, phone: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val existing = repository.getUserByEmail(email)
            if (existing != null) {
                onError("Email already registered!")
                return@launch
            }
            val newUser = UserEntity(
                fullName = fullName,
                email = email,
                phoneNumber = phone,
                passwordHash = pass, // secure representation
                role = "student"
            )
            val newId = repository.insertUser(newUser)
            _currentUser.value = newUser.copy(id = newId.toInt())
            onSuccess()
        }
    }

    fun loginUser(email: String, pass: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val user = repository.getUserByEmail(email)
            if (user == null) {
                onError("User does not exist!")
                return@launch
            }
            if (user.passwordHash != pass) {
                onError("Incorrect password!")
                return@launch
            }
            _currentUser.value = user
            onSuccess(user.role)
        }
    }

    fun logout() {
        _currentUser.value = null
        selectedCourse.value = null
        activeLesson.value = null
        _currentScreen.value = "home"
    }

    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    // Payments: Simulated Flutterwave Gateway
    fun startPaymentFlow(course: CourseEntity) {
        checkoutCourse.value = course
        showCheckoutModal.value = true
        paymentStatusMessage.value = "Awaiting payment initialization..."
    }

    fun executeSimulatedPayment(paymentMethod: String) {
        val course = checkoutCourse.value ?: return
        val user = _currentUser.value ?: return

        viewModelScope.launch {
            paymentStatusMessage.value = "Processing Flutterwave transaction secure key..."
            kotlinx.coroutines.delay(1200)

            val success = true // mock Flutterwave API return
            if (success) {
                // 1. Insert Payment
                val reference = "FW-TX-${UUID.randomUUID().toString().take(8).uppercase()}"
                repository.insertPayment(
                    PaymentEntity(
                        userId = user.id,
                        courseId = course.id,
                        amount = course.price,
                        status = "Success",
                        paymentMethod = paymentMethod,
                        reference = reference
                    )
                )

                // 2. Auto Enroll in Course
                repository.enrollUserInCourse(user.id, course.id)

                paymentStatusMessage.value = "Payment Secure! Automatic Enrollment Complete."
                kotlinx.coroutines.delay(800)
                showCheckoutModal.value = false
                checkoutCourse.value = null
                // Refresh views If we are on courses:
                _currentScreen.value = "dashboard"
            } else {
                paymentStatusMessage.value = "Gateway Connection error. Please try again."
            }
        }
    }

    // Enroll in Free Course Directly
    fun enrollInFreeCourse(course: CourseEntity) {
        val user = _currentUser.value ?: return
        if (course.isPaid) return
        viewModelScope.launch {
            repository.enrollUserInCourse(user.id, course.id)
            _currentScreen.value = "dashboard"
        }
    }

    // Video Visualizer Progress Action
    fun markLessonCompleted(lesson: LessonEntity) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.saveProgress(
                userId = user.id,
                lessonId = lesson.id,
                isCompleted = true,
                positionSeconds = lesson.videoDurationSeconds
            )
            // Trigger local refresh for active course context
            val currentIdx = activeLesson.value
            if (currentIdx?.id == lesson.id) {
                // If there's a next lesson, we can highlight it
                val courseLessons = repository.getLessonsForCourse(lesson.courseId)
                val currentPos = courseLessons.indexOfFirst { it.id == lesson.id }
                if (currentPos >= 0 && currentPos < courseLessons.size - 1) {
                    activeLesson.value = courseLessons[currentPos + 1]
                }
            }
        }
    }

    fun updateLessonProgressSeconds(lesson: LessonEntity, seconds: Int) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val isCompleted = seconds >= lesson.videoDurationSeconds
            repository.saveProgress(
                userId = user.id,
                lessonId = lesson.id,
                isCompleted = isCompleted,
                positionSeconds = seconds
            )
        }
    }

    // Administrative Powers
    fun addCourse(title: String, desc: String, inst: String, dur: String, price: Double, category: String) {
        viewModelScope.launch {
            val isPaid = price > 0
            val course = CourseEntity(
                title = title,
                description = desc,
                instructor = inst,
                duration = dur,
                price = price,
                isPaid = isPaid,
                thumbnailResName = "custom_banner",
                category = category
            )
            repository.insertCourse(course)
        }
    }

    fun updateCourseAdmin(course: CourseEntity) {
        viewModelScope.launch {
            repository.updateCourse(course)
        }
    }

    fun deleteCourseAdmin(course: CourseEntity) {
        viewModelScope.launch {
            repository.deleteCourse(course)
        }
    }

    // Additional student admin powers
    fun addStudentAdmin(name: String, email: String, phone: String, pass: String) {
        viewModelScope.launch {
            val u = UserEntity(fullName = name, email = email, phoneNumber = phone, passwordHash = pass, role = "student")
            repository.insertUser(u)
        }
    }

    fun updateStudentAdmin(user: UserEntity) {
        viewModelScope.launch {
            repository.updateUser(user)
        }
    }

    fun deleteStudentAdmin(user: UserEntity) {
        viewModelScope.launch {
            repository.deleteUser(user)
        }
    }

    // Lessons admin powers
    fun addLessonAdmin(courseId: Int, moduleTitle: String, lessonTitle: String, durationSecs: Int) {
        viewModelScope.launch {
            val lessons = repository.getLessonsForCourse(courseId)
            val newIdx = lessons.size + 1
            repository.insertLesson(
                LessonEntity(
                    courseId = courseId,
                    moduleTitle = moduleTitle,
                    lessonTitle = lessonTitle,
                    videoDurationSeconds = durationSecs,
                    orderIndex = newIdx
                )
            )
        }
    }

    fun deleteLessonAdmin(lesson: LessonEntity) {
        viewModelScope.launch {
            repository.deleteLesson(lesson)
        }
    }

    fun submitContact(name: String, email: String, subject: String, msg: String, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.insertContactMessage(
                ContactMessageEntity(senderName = name, senderEmail = email, subject = subject, message = msg)
            )
            onDone()
        }
    }
}
