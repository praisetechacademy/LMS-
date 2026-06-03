package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ==========================================
// 1. DATABASE ENTITIES
// ==========================================

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fullName: String,
    val email: String,
    val phoneNumber: String,
    val passwordHash: String, // simple hashed / store in clear for demo securely
    val role: String = "student" // "student" or "admin"
)

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val instructor: String,
    val duration: String,
    val price: Double,
    val isPaid: Boolean,
    val thumbnailResName: String, // we can map to drawables or shapes
    val category: String
)

@Entity(tableName = "lessons")
data class LessonEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val courseId: Int,
    val moduleTitle: String,
    val lessonTitle: String,
    val videoDurationSeconds: Int,
    val orderIndex: Int
)

@Entity(tableName = "enrollments")
data class EnrollmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val courseId: Int,
    val enrollmentDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val courseId: Int,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val status: String, // "Success", "Pending", "Failed"
    val paymentMethod: String, // "Card", "Bank Transfer", "USSD"
    val reference: String
)

@Entity(tableName = "certificates")
data class CertificateEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val courseId: Int,
    val issueDate: Long = System.currentTimeMillis(),
    val certificateCode: String
)

@Entity(tableName = "progress_tracking", primaryKeys = ["userId", "lessonId"])
data class ProgressEntity(
    val userId: Int,
    val lessonId: Int,
    val isCompleted: Boolean,
    val lastPositionSeconds: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "testimonials")
data class TestimonialEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentName: String,
    val courseTitle: String,
    val feedback: String,
    val rating: Int = 5
)

@Entity(tableName = "contact_messages")
data class ContactMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val senderName: String,
    val senderEmail: String,
    val subject: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

// ==========================================
// 2. DATA ACCESS OBJECT WORKERS (DAOs)
// ==========================================

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun getUserByIdFlow(id: Int): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Int): UserEntity?

    @Query("SELECT * FROM users WHERE role = 'student'")
    fun getAllStudentsFlow(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)
}

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses ORDER BY id DESC")
    fun getAllCoursesFlow(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE id = :id")
    suspend fun getCourseById(id: Int): CourseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: CourseEntity): Long

    @Update
    suspend fun updateCourse(course: CourseEntity)

    @Delete
    suspend fun deleteCourse(course: CourseEntity)

    // Lessons
    @Query("SELECT * FROM lessons WHERE courseId = :courseId ORDER BY orderIndex ASC")
    fun getLessonsForCourseFlow(courseId: Int): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE courseId = :courseId ORDER BY orderIndex ASC")
    suspend fun getLessonsForCourse(courseId: Int): List<LessonEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLesson(lesson: LessonEntity): Long

    @Query("DELETE FROM lessons WHERE courseId = :courseId")
    suspend fun deleteLessonsForCourse(courseId: Int)

    @Delete
    suspend fun deleteLesson(lesson: LessonEntity)
}

@Dao
interface EnrollmentDao {
    @Query("SELECT * FROM enrollments WHERE userId = :userId")
    fun getEnrollmentsForUserFlow(userId: Int): Flow<List<EnrollmentEntity>>

    @Query("SELECT * FROM enrollments WHERE userId = :userId AND courseId = :courseId LIMIT 1")
    suspend fun getEnrollment(userId: Int, courseId: Int): EnrollmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEnrollment(enrollment: EnrollmentEntity): Long

    @Query("DELETE FROM enrollments WHERE userId = :userId AND courseId = :courseId")
    suspend fun deleteEnrollment(userId: Int, courseId: Int)
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY date DESC")
    fun getAllPaymentsFlow(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE userId = :userId ORDER BY date DESC")
    fun getPaymentsForUserFlow(userId: Int): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity): Long

    @Update
    suspend fun updatePayment(payment: PaymentEntity)
}

@Dao
interface CertificateDao {
    @Query("SELECT * FROM certificates ORDER BY issueDate DESC")
    fun getAllCertificatesFlow(): Flow<List<CertificateEntity>>

    @Query("SELECT * FROM certificates WHERE userId = :userId")
    fun getCertificatesForUserFlow(userId: Int): Flow<List<CertificateEntity>>

    @Query("SELECT * FROM certificates WHERE userId = :userId AND courseId = :courseId LIMIT 1")
    suspend fun getCertificate(userId: Int, courseId: Int): CertificateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCertificate(certificate: CertificateEntity): Long
}

@Dao
interface ProgressDao {
    @Query("SELECT * FROM progress_tracking WHERE userId = :userId")
    fun getProgressForUserFlow(userId: Int): Flow<List<ProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: ProgressEntity)

    @Query("SELECT * FROM progress_tracking WHERE userId = :userId AND lessonId = :lessonId LIMIT 1")
    suspend fun getProgressForLesson(userId: Int, lessonId: Int): ProgressEntity?
}

@Dao
interface FeedbackDao {
    @Query("SELECT * FROM testimonials ORDER BY id DESC")
    fun getAllTestimonialsFlow(): Flow<List<TestimonialEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTestimonial(testimonial: TestimonialEntity)

    @Query("SELECT * FROM contact_messages ORDER BY timestamp DESC")
    fun getAllContactMessagesFlow(): Flow<List<ContactMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContactMessage(message: ContactMessageEntity)
}

// ==========================================
// 3. DATABASE CONTAINER
// ==========================================

@Database(
    entities = [
        UserEntity::class,
        CourseEntity::class,
        LessonEntity::class,
        EnrollmentEntity::class,
        PaymentEntity::class,
        CertificateEntity::class,
        ProgressEntity::class,
        TestimonialEntity::class,
        ContactMessageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LmsDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun courseDao(): CourseDao
    abstract fun enrollmentDao(): EnrollmentDao
    abstract fun paymentDao(): PaymentDao
    abstract fun certificateDao(): CertificateDao
    abstract fun progressDao(): ProgressDao
    abstract fun feedbackDao(): FeedbackDao

    companion object {
        @Volatile
        private var INSTANCE: LmsDatabase? = null

        fun getDatabase(context: Context): LmsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LmsDatabase::class.java,
                    "praisetech_lms_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
