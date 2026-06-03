package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

class LmsRepository(private val db: LmsDatabase) {

    private val userDao = db.userDao()
    private val courseDao = db.courseDao()
    private val enrollmentDao = db.enrollmentDao()
    private val paymentDao = db.paymentDao()
    private val certificateDao = db.certificateDao()
    private val progressDao = db.progressDao()
    private val feedbackDao = db.feedbackDao()

    // Users
    suspend fun getUserByEmail(email: String): UserEntity? = userDao.getUserByEmail(email)
    fun getUserByIdFlow(id: Int): Flow<UserEntity?> = userDao.getUserByIdFlow(id)
    suspend fun getUserById(id: Int): UserEntity? = userDao.getUserById(id)
    fun getAllStudentsFlow(): Flow<List<UserEntity>> = userDao.getAllStudentsFlow()
    suspend fun insertUser(user: UserEntity): Long = userDao.insertUser(user)
    suspend fun updateUser(user: UserEntity) = userDao.updateUser(user)
    suspend fun deleteUser(user: UserEntity) = userDao.deleteUser(user)

    // Courses & Lessons
    fun getAllCoursesFlow(): Flow<List<CourseEntity>> = courseDao.getAllCoursesFlow()
    suspend fun getCourseById(id: Int): CourseEntity? = courseDao.getCourseById(id)
    suspend fun insertCourse(course: CourseEntity): Long = courseDao.insertCourse(course)
    suspend fun updateCourse(course: CourseEntity) = courseDao.updateCourse(course)
    suspend fun deleteCourse(course: CourseEntity) {
        courseDao.deleteLessonsForCourse(course.id)
        courseDao.deleteCourse(course)
    }

    fun getLessonsForCourseFlow(courseId: Int): Flow<List<LessonEntity>> = courseDao.getLessonsForCourseFlow(courseId)
    suspend fun getLessonsForCourse(courseId: Int): List<LessonEntity> = courseDao.getLessonsForCourse(courseId)
    suspend fun insertLesson(lesson: LessonEntity): Long = courseDao.insertLesson(lesson)
    suspend fun deleteLesson(lesson: LessonEntity) = courseDao.deleteLesson(lesson)

    // Enrollments
    fun getEnrollmentsForUserFlow(userId: Int): Flow<List<EnrollmentEntity>> = enrollmentDao.getEnrollmentsForUserFlow(userId)
    suspend fun getEnrollment(userId: Int, courseId: Int): EnrollmentEntity? = enrollmentDao.getEnrollment(userId, courseId)
    suspend fun enrollUserInCourse(userId: Int, courseId: Int) {
        if (enrollmentDao.getEnrollment(userId, courseId) == null) {
            enrollmentDao.insertEnrollment(EnrollmentEntity(userId = userId, courseId = courseId))
        }
    }
    suspend fun cancelEnrollment(userId: Int, courseId: Int) = enrollmentDao.deleteEnrollment(userId, courseId)

    // Payments
    fun getAllPaymentsFlow(): Flow<List<PaymentEntity>> = paymentDao.getAllPaymentsFlow()
    fun getPaymentsForUserFlow(userId: Int): Flow<List<PaymentEntity>> = paymentDao.getPaymentsForUserFlow(userId)
    suspend fun insertPayment(payment: PaymentEntity): Long = paymentDao.insertPayment(payment)
    suspend fun updatePayment(payment: PaymentEntity) = paymentDao.updatePayment(payment)

    // Certificates
    fun getAllCertificatesFlow(): Flow<List<CertificateEntity>> = certificateDao.getAllCertificatesFlow()
    fun getCertificatesForUserFlow(userId: Int): Flow<List<CertificateEntity>> = certificateDao.getCertificatesForUserFlow(userId)
    suspend fun getCertificate(userId: Int, courseId: Int): CertificateEntity? = certificateDao.getCertificate(userId, courseId)
    suspend fun issueCertificate(userId: Int, courseId: Int) {
        if (certificateDao.getCertificate(userId, courseId) == null) {
            val code = "PTS-${courseId}-${1000 + (Math.random() * 8999).toInt()}"
            certificateDao.insertCertificate(
                CertificateEntity(userId = userId, courseId = courseId, certificateCode = code)
            )
        }
    }

    // Progress Tracking & Auto Certificate Unlock
    fun getProgressForUserFlow(userId: Int): Flow<List<ProgressEntity>> = progressDao.getProgressForUserFlow(userId)
    suspend fun getProgressForLesson(userId: Int, lessonId: Int): ProgressEntity? = progressDao.getProgressForLesson(userId, lessonId)
    
    suspend fun saveProgress(userId: Int, lessonId: Int, isCompleted: Boolean, positionSeconds: Int) {
        val progress = ProgressEntity(
            userId = userId,
            lessonId = lessonId,
            isCompleted = isCompleted,
            lastPositionSeconds = positionSeconds,
            updatedAt = System.currentTimeMillis()
        )
        progressDao.saveProgress(progress)

        // Check if all lessons of this course are complete to AUTO-UNLOCK certificate!
        val lesson = db.openHelper.writableDatabase // let's find courseId from lesson info
        // Simple select: get courseId of this lesson
        val lessonsOfThisCourse = getCourseIdByLessonId(lessonId)?.let { courseId ->
            val allLessons = courseDao.getLessonsForCourse(courseId)
            val userProgressList = progressDao.getProgressForUserFlow(userId).first()
            val completedLessonIds = userProgressList.filter { it.isCompleted }.map { it.lessonId }
            
            val allCompleted = allLessons.isNotEmpty() && allLessons.all { it.id in completedLessonIds }
            if (allCompleted) {
                issueCertificate(userId, courseId)
            }
        }
    }

    private suspend fun getCourseIdByLessonId(lessonId: Int): Int? {
        val courses = courseDao.getAllCoursesFlow().first()
        for (c in courses) {
            val lessons = courseDao.getLessonsForCourse(c.id)
            if (lessons.any { it.id == lessonId }) {
                return c.id
            }
        }
        return null
    }

    // Feedback (Testimonials & Contact Messages)
    fun getAllTestimonialsFlow(): Flow<List<TestimonialEntity>> = feedbackDao.getAllTestimonialsFlow()
    suspend fun insertTestimonial(testimonial: TestimonialEntity) = feedbackDao.insertTestimonial(testimonial)

    fun getAllContactMessagesFlow(): Flow<List<ContactMessageEntity>> = feedbackDao.getAllContactMessagesFlow()
    suspend fun insertContactMessage(message: ContactMessageEntity) = feedbackDao.insertContactMessage(message)

    // Dynamic Database Seeding
    suspend fun seedDatabaseIfEmpty() {
        // 1. Check if users are empty
        val existingCourses = courseDao.getAllCoursesFlow().first()
        if (existingCourses.isNotEmpty()) return // already seeded

        // Seed default Admin
        val adminUser = UserEntity(
            fullName = "Main Administrator",
            email = "admin@praisetech.com",
            phoneNumber = "+2348012345678",
            passwordHash = "admin", // simple security for demo admin access
            role = "admin"
        )
        userDao.insertUser(adminUser)

        // Seed standard student
        val studentUser = UserEntity(
            fullName = "Tunde Praise",
            email = "student@praisetech.com",
            phoneNumber = "+2348098765432",
            passwordHash = "student",
            role = "student"
        )
        userDao.insertUser(studentUser)

        // Seed courses
        val c1Id = courseDao.insertCourse(CourseEntity(
            title = "Jetpack Compose Masters",
            description = "Build visually gorgeous, fully reactive Android applications using Kotlin & Jetpack Compose. Learn flows, state holders, animations, and material design.",
            instructor = "Prof. Praise Victor",
            duration = "12 Hours",
            price = 15000.0,
            isPaid = true,
            thumbnailResName = "compose_banner",
            category = "Mobile Development"
        )).toInt()

        val c2Id = courseDao.insertCourse(CourseEntity(
            title = "Modern Full Stack Web Engineering",
            description = "Develop enterprise web applications from database architecture up to styled interfaces using HTML5, CSS3, JS, PHP, and secure MySQL backend services.",
            instructor = "Engr. Timothy Alao",
            duration = "24 Hours",
            price = 20000.0,
            isPaid = true,
            thumbnailResName = "web_banner",
            category = "Software Engineering"
        )).toInt()

        val c3Id = courseDao.insertCourse(CourseEntity(
            title = "Essentials of Cloud & AWS DevOps",
            description = "A comprehensive hands-on primer to cloud resource provisioning, AWS cloud deployment pipelines, containerization, and foundational system engineering.",
            instructor = "Aina Praise Solution",
            duration = "6 Hours",
            price = 0.0,
            isPaid = false,
            thumbnailResName = "cloud_banner",
            category = "Cloud Computing"
        )).toInt()

        val c4Id = courseDao.insertCourse(CourseEntity(
            title = "Digital Branding & Growth Architecture",
            description = "Master organic SEO scaling indices, structured Google campaigns, brand representation models, and telemetry analysis workflows.",
            instructor = "Mrs. Sarah Jenkins",
            duration = "8 Hours",
            price = 5000.0,
            isPaid = true,
            thumbnailResName = "marketing_banner",
            category = "Digital Business"
        )).toInt()

        // Seed lessons for Course 1
        courseDao.insertLesson(LessonEntity(courseId = c1Id, moduleTitle = "Module 1: Getting Started", lessonTitle = "Introduction to Declarative Compose Architectures", videoDurationSeconds = 120, orderIndex = 1))
        courseDao.insertLesson(LessonEntity(courseId = c1Id, moduleTitle = "Module 1: Getting Started", lessonTitle = "Mastering Compose State & Recompositions", videoDurationSeconds = 180, orderIndex = 2))
        courseDao.insertLesson(LessonEntity(courseId = c1Id, moduleTitle = "Module 2: Sophisticated Layouts", lessonTitle = "Building Dynamic Custom Grids & Overlapping Views", videoDurationSeconds = 240, orderIndex = 3))

        // Seed lessons for Course 2
        courseDao.insertLesson(LessonEntity(courseId = c2Id, moduleTitle = "Module 1: Web Interface Basics", lessonTitle = "Writing Semantic HTML5 & Modern Responsive CSS3", videoDurationSeconds = 150, orderIndex = 1))
        courseDao.insertLesson(LessonEntity(courseId = c2Id, moduleTitle = "Module 1: Web Interface Basics", lessonTitle = "Dynamic DOM Manipulations & Event Listeners in JS", videoDurationSeconds = 210, orderIndex = 2))
        courseDao.insertLesson(LessonEntity(courseId = c2Id, moduleTitle = "Module 2: Server Architecture", lessonTitle = "Designing Secure PHP Routing & Request Parsers", videoDurationSeconds = 300, orderIndex = 3))
        courseDao.insertLesson(LessonEntity(courseId = c2Id, moduleTitle = "Module 2: Server Architecture", lessonTitle = "MySQL Relational Schemas & Safe Prepared Statements", videoDurationSeconds = 350, orderIndex = 4))

        // Seed lessons for Course 3
        courseDao.insertLesson(LessonEntity(courseId = c3Id, moduleTitle = "Module 1: Cloud Foundational Concepts", lessonTitle = "What is Cloud Computing & AWS Services Overview", videoDurationSeconds = 90, orderIndex = 1))
        courseDao.insertLesson(LessonEntity(courseId = c3Id, moduleTitle = "Module 1: Cloud Foundational Concepts", lessonTitle = "Dockerizing Applications & Running Local Clusters", videoDurationSeconds = 140, orderIndex = 2))

        // Seed lessons for Course 4
        courseDao.insertLesson(LessonEntity(courseId = c4Id, moduleTitle = "Module 1: Branding Indices", lessonTitle = "Creating a High-Impact Brand Identity", videoDurationSeconds = 110, orderIndex = 1))
        courseDao.insertLesson(LessonEntity(courseId = c4Id, moduleTitle = "Module 1: Branding Indices", lessonTitle = "Configuring SEO Optimization Tagging Architectures", videoDurationSeconds = 160, orderIndex = 2))
        courseDao.insertLesson(LessonEntity(courseId = c4Id, moduleTitle = "Module 2: Telemetry", lessonTitle = "Setting Up Dynamic Analytical Reports on Google Console", videoDurationSeconds = 220, orderIndex = 3))

        // Seed testimonials
        feedbackDao.insertTestimonial(TestimonialEntity(
            studentName = "Oluwaseun Adeboye",
            courseTitle = "Jetpack Compose Masters",
            feedback = "Praise Tech Solution transformed my career! The lessons are straight-to-the-point, clear, and extremely practical.",
            rating = 5
        ))
        feedbackDao.insertTestimonial(TestimonialEntity(
            studentName = "Amarachi Kalu",
            courseTitle = "Modern Full Stack Web Engineering",
            feedback = "I went from coding simple HTML to deploying a dynamic PHP-MySQL app. Exceptional content and direct tutor support!",
            rating = 5
        ))
        feedbackDao.insertTestimonial(TestimonialEntity(
            studentName = "Musa Ibrahim",
            courseTitle = "Essentials of Cloud & AWS DevOps",
            feedback = "The DevOps training is first-rate. It is amazing how much value Praise Tech packs into a free course.",
            rating = 4
        ))
    }
}
