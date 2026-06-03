package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.LmsViewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: LmsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val darkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
            MyApplicationTheme(darkTheme = darkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LmsAppContent(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LmsAppContent(viewModel: LmsViewModel) {
    val coroutineScope = rememberCoroutineScope()
    val localCtx = LocalContext.current
    val currentScreen by viewModel.currentScreenState.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUserState.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

    val showCheckout by viewModel.showCheckoutModal.collectAsStateWithLifecycle()
    val checkoutCourse by viewModel.checkoutCourse.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            LmsTopBar(
                viewModel = viewModel,
                currentUser = currentUser,
                isDarkMode = isDarkMode,
                onToggleDarkMode = { viewModel.isDarkMode.value = !isDarkMode }
            )
        },
        bottomBar = {
            LmsBottomBar(
                currentScreen = currentScreen,
                currentUser = currentUser,
                onNavigate = { viewModel.navigateTo(it) }
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut()
                },
                label = "ScreenTransition"
            ) { targetScreen ->
                when (targetScreen) {
                    "home" -> HomeScreen(viewModel)
                    "about" -> AboutScreen(viewModel)
                    "courses" -> CoursesScreen(viewModel)
                    "contact" -> ContactScreen(viewModel)
                    "auth" -> AuthScreen(viewModel)
                    "dashboard" -> StudentDashboardScreen(viewModel)
                    "learning_area" -> CourseLearningAreaScreen(viewModel)
                    "admin_dashboard" -> AdminDashboardScreen(viewModel)
                }
            }

            // Global Flutterwave Payment Gateway Simulator Modal Dialog
            if (showCheckout && checkoutCourse != null) {
                FlutterwaveCheckoutDialog(
                    course = checkoutCourse!!,
                    viewModel = viewModel,
                    onDismiss = { viewModel.showCheckoutModal.value = false }
                )
            }
        }
    }
}

// ==========================================
// NAVIGATION COMPONENTS
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LmsTopBar(
    viewModel: LmsViewModel,
    currentUser: UserEntity?,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit
) {
    // Elegant background color based on theme: always sleek NavalBlue (#001F3F) or customized slate
    val containerColor = if (isDarkMode) Color(0xFF0F172A) else Color(0xFF001F3F)
    val contentColor = Color.White
    val blueAccent = Color(0xFF60A5FA)

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "Logo",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Praise Tech",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Solution",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = blueAccent
                        )
                    }
                }
            },
            actions = {
                IconButton(
                    onClick = onToggleDarkMode,
                    modifier = Modifier.testTag("dark_mode_button")
                ) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle Dark Mode",
                        tint = Color.White
                    )
                }

                if (currentUser != null) {
                    AssistChip(
                        onClick = {
                            if (currentUser.role == "admin") {
                                viewModel.navigateTo("admin_dashboard")
                            } else {
                                viewModel.navigateTo("dashboard")
                            }
                        },
                        label = { 
                            Text(
                                text = if (currentUser.role == "admin") "Admin" else currentUser.fullName.split(" ").firstOrNull() ?: "Student",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ) 
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (currentUser.role == "admin") Icons.Default.AdminPanelSettings else Icons.Default.AccountCircle,
                                contentDescription = "Profile",
                                tint = blueAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color.White.copy(alpha = 0.1f),
                            labelColor = Color.White,
                            leadingIconContentColor = blueAccent
                        ),
                        modifier = Modifier.padding(end = 6.dp)
                    )

                    IconButton(onClick = { viewModel.logout() }) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout",
                            tint = Color(0xFFEF4444)
                        )
                    }
                } else {
                    Button(
                        onClick = { viewModel.navigateTo("auth") },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = blueAccent,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 8.dp).testTag("header_login_button")
                    ) {
                        Text("Join Free", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = Color.White,
                actionIconContentColor = Color.White
            )
        )
    }
}

@Composable
fun LmsBottomBar(
    currentScreen: String,
    currentUser: UserEntity?,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        val darkPinkColor = MaterialTheme.colorScheme.primary

        NavigationBarItem(
            selected = currentScreen == "home",
            onClick = { onNavigate("home") },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home", fontSize = 10.sp) },
            modifier = Modifier.testTag("nav_home")
        )

        NavigationBarItem(
            selected = currentScreen == "courses",
            onClick = { onNavigate("courses") },
            icon = { Icon(Icons.Default.Book, contentDescription = "Courses") },
            label = { Text("Courses", fontSize = 10.sp) },
            modifier = Modifier.testTag("nav_courses")
        )

        NavigationBarItem(
            selected = currentScreen == "about",
            onClick = { onNavigate("about") },
            icon = { Icon(Icons.Default.Info, contentDescription = "About") },
            label = { Text("About", fontSize = 10.sp) },
            modifier = Modifier.testTag("nav_about")
        )

        NavigationBarItem(
            selected = currentScreen == "contact",
            onClick = { onNavigate("contact") },
            icon = { Icon(Icons.Default.Mail, contentDescription = "Contact") },
            label = { Text("Contact", fontSize = 10.sp) },
            modifier = Modifier.testTag("nav_contact")
        )

        // Conditional tab based on authentication roles
        if (currentUser != null) {
            val targetDashboard = if (currentUser.role == "admin") "admin_dashboard" else "dashboard"
            NavigationBarItem(
                selected = currentScreen == targetDashboard || currentScreen == "learning_area",
                onClick = { onNavigate(targetDashboard) },
                icon = { 
                    Icon(
                        imageVector = if (currentUser.role == "admin") Icons.Default.AdminPanelSettings else Icons.Default.Dashboard, 
                        contentDescription = "Dashboard"
                    ) 
                },
                label = { Text("LMS Pro", fontSize = 10.sp) },
                modifier = Modifier.testTag("nav_dashboard")
            )
        } else {
            NavigationBarItem(
                selected = currentScreen == "auth",
                onClick = { onNavigate("auth") },
                icon = { Icon(Icons.Default.Login, contentDescription = "Sign In") },
                label = { Text("Sign In", fontSize = 10.sp) },
                modifier = Modifier.testTag("nav_signin")
            )
        }
    }
}

// ==========================================
// 1. HOME SCREEN PARENT
// ==========================================

@Composable
fun HomeScreen(viewModel: LmsViewModel) {
    val courses by viewModel.allCourses.collectAsStateWithLifecycle()
    val testimonials by viewModel.testimonialsList.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUserState.collectAsStateWithLifecycle()

    var emailSubState by remember { mutableStateOf("") }
    var subSuccessState by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp, horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Hero Section Call to Action banner with rich graphics styling
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "PREMIUM LMS PLATFORM",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    Text(
                        text = "Empower Your Tech Trajectory",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        lineHeight = 34.sp
                    )
                    Text(
                        text = "Learn software architecture, Jetpack Compose UI, and relational backend systems from industry mentors. Free signup. Premium access credentials.",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                if (currentUser != null) {
                                    viewModel.navigateTo("courses")
                                } else {
                                    viewModel.navigateTo("auth")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("hero_cta_explore")
                        ) {
                            Text("Browse Courses", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.navigateTo("about") },
                            border = BorderStroke(1.dp, Color.White),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("Learn More")
                        }
                    }
                }
            }
        }

        // About the Brand Overview Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "Badge",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "About Praise Tech Solution",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = "We are an innovative online academy supplying top-tier technical skills globally. Our interactive course paths utilize structured learning modules, precise visual progress trackers, and automagic certificate awards matching verified standards.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Why Choose Us Section Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Why Choose Praise Tech?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WhyCard(
                        icon = Icons.Default.VideoLibrary,
                        title = "Mentor Lessons",
                        desc = "Self-placed high quality video uploads.",
                        modifier = Modifier.weight(1f)
                    )
                    WhyCard(
                        icon = Icons.Default.Payments,
                        title = "Flutterwave",
                        desc = "Instantly secure payments checkout.",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WhyCard(
                        icon = Icons.Default.WorkspacePremium,
                        title = "Certificates",
                        desc = "Automatic download on completion.",
                        modifier = Modifier.weight(1f)
                    )
                    WhyCard(
                        icon = Icons.Default.SupportAgent,
                        title = "24/7 Support",
                        desc = "Contact our team for direct advisory.",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Featured Courses horizontal preview
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Featured Courses",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )

                    TextButton(onClick = { viewModel.navigateTo("courses") }) {
                        Text("See All", color = MaterialTheme.colorScheme.primary)
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Arrow",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (courses.isEmpty()) {
                    Text("No pre-seeded courses found.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(courses.take(3)) { course ->
                            HomeCourseCard(course = course, onClick = {
                                viewModel.selectedCourse.value = course
                                viewModel.navigateTo("courses")
                            })
                        }
                    }
                }
            }
        }

        // Student Testimonials Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "What Students Say",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(testimonials) { item ->
                        TestimonialDisplayItem(item)
                    }
                }
            }
        }

        // Frequently Asked Questions Section accordion
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Frequently Asked Questions",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                FaqAccordionItem(
                    q = "Is registration free?",
                    a = "Yes! You can register instantly without any registration fee. You can browse all courses, lessons, and modules completely for free."
                )

                FaqAccordionItem(
                    q = "How do I access premium courses?",
                    a = "Premium courses require a one-off payment. You can pay securely using the built-in Flutterwave checkout portal supporting cards, transfers, and USSD."
                )

                FaqAccordionItem(
                    q = "When do I get my certificate?",
                    a = "Certificates are automatically unlocked the moment you finish watching and marking all lessons inside a course path as complete!"
                )
            }
        }

        // Newsletter Subscription form
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.MarkEmailRead,
                        contentDescription = "Mail",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )

                    Text(
                        text = "Join Our Technical Newsletter",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Receive code updates, technological bulletins, and discount coupons for upcoming Praise Tech releases.",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!subSuccessState) {
                        OutlinedTextField(
                            value = emailSubState,
                            onValueChange = { emailSubState = it },
                            placeholder = { Text("Your Email Address") },
                            modifier = Modifier.fillMaxWidth().testTag("newsletter_email"),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Button(
                            onClick = {
                                if (emailSubState.contains("@")) {
                                    subSuccessState = true
                                    emailSubState = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("newsletter_subscribe"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Subscribe Now")
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Subscription Confirmed! Thank you.",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Home Footer Section details
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "© 2026 Praise Tech Solution.",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Primary colors: Strategic Navy Blue & Pure White.",
                    fontSize = 10.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(Icons.Default.Share, "FB", modifier = Modifier.size(16.dp))
                    Icon(Icons.Default.Language, "Web", modifier = Modifier.size(16.dp))
                    Icon(Icons.Default.Support, "Support", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun WhyCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon, 
                    contentDescription = title, 
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(text = desc, fontSize = 11.sp, lineHeight = 15.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun HomeCourseCard(course: CourseEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Mock banner graphic using canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Laptop,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = course.category.uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )

                Text(
                    text = course.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = if (course.isPaid) "₦${String.format("%,.0f", course.price)}" else "FREE",
                    color = if (course.isPaid) MaterialTheme.colorScheme.primary else Color(0xFF10B981),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun TestimonialDisplayItem(item: TestimonialEntity) {
    Card(
        modifier = Modifier.width(260.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(item.rating) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Star",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = "\"${item.feedback}\"",
                fontSize = 12.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            Text(
                text = item.studentName,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Text(
                text = "Course: ${item.courseTitle}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun FaqAccordionItem(q: String, a: String) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = q,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Toggle"
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = a,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// ==========================================
// 2. ABOUT SCREEN
// ==========================================

@Composable
fun AboutScreen(viewModel: LmsViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "About Our Academy",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Company Overview", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    text = "Praise Tech Solution is a leading LMS provider tailored towards digital empowerment. We assemble high-quality teaching curricula, deployable modules, and professional visual feedback trackers enabling candidates to gain practical skills fast.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1.5f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Our Mission", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    Text("To supply structured, accessible, and self-placed learning infrastructure globally, accelerating entry ratios into technology professions.", fontSize = 11.sp, lineHeight = 15.sp)
                }
            }

            Card(
                modifier = Modifier.weight(1.5f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Our Vision", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    Text("To build Africa's premier technology incubator portal where standard certifications correspond to rigorous, verified learning mastery.", fontSize = 11.sp, lineHeight = 15.sp)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Services Offered", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                ServiceBullet(title = "Self-Paced Video Learning", desc = "Clear organized lectures matching modern technology vectors.")
                ServiceBullet(title = "Instant Payment Enrolling", desc = "Secured payments directly integrated with Flutterwave gateways.")
                ServiceBullet(title = "Automated Professional Grading", desc = "Unlock graduation certificates the instant path lessons are complete.")
                ServiceBullet(title = "Expert Tutor Consultation", desc = "Direct lines to professional course managers and grading staff.")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Corporate Location & Info", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 14.sp)
                Text("• Headquarters: Praise Tech Tech-Hub, Lagos, Nigeria", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("• Email: support@praisetech.com", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("• Phone: +234 803 123 4567", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
fun ServiceBullet(title: String, desc: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Default.CheckCircle, "Bullet", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
        }
    }
}

// ==========================================
// 3. COURSES SCREEN WITH SEARCH & FILTER
// ==========================================

@Composable
fun CoursesScreen(viewModel: LmsViewModel) {
    val courses by viewModel.allCourses.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val currUser by viewModel.currentUserState.collectAsStateWithLifecycle()
    val enrollments by viewModel.userEnrollments.collectAsStateWithLifecycle()

    var showDetailsSheet by remember { mutableStateOf<CourseEntity?>(null) }

    val categories = listOf("All", "Mobile Development", "Software Engineering", "Cloud Computing", "Digital Business")

    // Filter courses locally
    val filteredCourses = courses.filter {
        (selectedCategory == "All" || it.category == selectedCategory) &&
                (it.title.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Browse Tech Courses",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Search Bar input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.searchQuery.value = it },
            placeholder = { Text("Search title, instructor or topic...") },
            leadingIcon = { Icon(Icons.Default.Search, "Search") },
            modifier = Modifier.fillMaxWidth().testTag("courses_search_bar"),
            shape = RoundedCornerShape(16.dp),
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                        Icon(Icons.Default.Clear, "Clear")
                    }
                }
            }
        )

        // Category Filter row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { category ->
                val isSelected = selectedCategory == category
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.selectedCategory.value = category },
                    label = { Text(category, fontSize = 11.sp) },
                    shape = RoundedCornerShape(24.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.testTag("filter_chip_$category")
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Courses list
        if (filteredCourses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Info, "Empty", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("No courses match your query.", fontWeight = FontWeight.Bold)
                    Text("Try standard search criteria.", fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredCourses) { course ->
                    val isEnrolled = enrollments.any { it.courseId == course.id }
                    CourseCatalogRowItem(
                        course = course,
                        isEnrolled = isEnrolled,
                        onClick = {
                            showDetailsSheet = course
                        }
                    )
                }
            }
        }
    }

    // Interactive details pop-up modal Sheet represent detailed lessons or direct buy
    if (showDetailsSheet != null) {
        CourseDetailsPopup(
            course = showDetailsSheet!!,
            viewModel = viewModel,
            isEnrolled = enrollments.any { it.courseId == showDetailsSheet!!.id },
            onDismiss = { showDetailsSheet = null }
        )
    }
}

@Composable
fun CourseCatalogRowItem(
    course: CourseEntity,
    isEnrolled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("course_row_${course.id}"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Simulated visual image
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LaptopMac,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (course.isPaid) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else Color(0xFF10B981).copy(alpha = 0.15f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (course.isPaid) "₦${String.format("%,.0f", course.price)}" else "FREE",
                            color = if (course.isPaid) MaterialTheme.colorScheme.primary else Color(0xFF10B981),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (isEnrolled) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFE0F2FE), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Enrolled", color = Color(0xFF0369A1), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Text(
                    text = course.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Instructor: ${course.instructor}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Text(
                    text = "Duration: ${course.duration}",
                    fontSize = 10.sp,
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open Details",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun CourseDetailsPopup(
    course: CourseEntity,
    viewModel: LmsViewModel,
    isEnrolled: Boolean,
    onDismiss: () -> Unit
) {
    val currentUser by viewModel.currentUserState.collectAsStateWithLifecycle()
    var lessonsList by remember { mutableStateOf<List<LessonEntity>>(emptyList()) }

    LaunchedEffect(course.id) {
        viewModel.repository.getLessonsForCourse(course.id).let {
            lessonsList = it
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = course.category,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Text(
                    text = course.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(text = course.description, fontSize = 13.sp, lineHeight = 18.sp)

                HorizontalDivider()

                Text("Course Structure: Modules & Lessons", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                if (lessonsList.isEmpty()) {
                    Text("No lessons set for this course yet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                } else {
                    val groupedLessons = lessonsList.groupBy { it.moduleTitle }
                    groupedLessons.forEach { (moduleTitle, lessons) ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = moduleTitle, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                lessons.forEach { les ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.PlayCircleOutline, "Play", modifier = Modifier.size(16.dp))
                                        Text(text = les.lessonTitle, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Price Tag", fontSize = 11.sp)
                        Text(
                            text = if (course.isPaid) "₦${String.format("%,.0f", course.price)}" else "FREE",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (isEnrolled) {
                        Button(
                            onClick = {
                                viewModel.selectedCourse.value = course
                                viewModel.navigateTo("learning_area")
                                onDismiss()
                            },
                            modifier = Modifier.testTag("action_continue_learning_details")
                        ) {
                            Text("Open Learning Area")
                        }
                    } else if (currentUser == null) {
                        Button(
                            onClick = {
                                viewModel.navigateTo("auth")
                                onDismiss()
                            },
                            modifier = Modifier.testTag("action_login_details")
                        ) {
                            Text("Unlock Course")
                        }
                    } else {
                        // User logged in, not enrolled
                        Button(
                            onClick = {
                                if (course.isPaid) {
                                    viewModel.startPaymentFlow(course)
                                } else {
                                    viewModel.enrollInFreeCourse(course)
                                }
                                onDismiss()
                            },
                            modifier = Modifier.testTag("action_enroll_details")
                        ) {
                            Text(if (course.isPaid) "Enroll with Flutterwave" else "Enroll for Free")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. CONTACT SCREEN WITH LIVE SUBMISSION
// ==========================================

@Composable
fun ContactScreen(viewModel: LmsViewModel) {
    var nameState by remember { mutableStateOf("") }
    var emailState by remember { mutableStateOf("") }
    var subjectState by remember { mutableStateOf("") }
    var msgState by remember { mutableStateOf("") }
    var isSubmitted by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Connect With Us",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Contact Information", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Phone, "Phone", tint = MaterialTheme.colorScheme.primary)
                    Text("+234 803 123 4567")
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Email, "Email", tint = MaterialTheme.colorScheme.primary)
                    Text("support@praisetech.com")
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.LocationOn, "Location", tint = MaterialTheme.colorScheme.primary)
                    Text("Praise Tech Hub, Admiralty Way, Lekki Phase 1, Lagos State, Nigeria")
                }
            }
        }

        // Location Map Section visually rendered with drawing Canvas for modern responsive look
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Academy Map Coordinates", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE2E8F0))
                    .drawBehind {
                        // Draw simulated roadmap vectors on map background
                        val path = Path().apply {
                            moveTo(0f, size.height / 3)
                            lineTo(size.width, size.height * 0.4f)
                            moveTo(size.width / 2, 0f)
                            lineTo(size.width / 3, size.height)
                        }
                        drawPath(path, color = Color(0xFF94A3B8), style = Stroke(width = 4f))
                        // Draw central pin marker
                        val pinPos = Offset(size.width * 0.45f, size.height * 0.45f)
                        drawCircle(color = Color(0xFFEF4444), radius = 10f, center = pinPos)
                        drawCircle(color = Color(0xFFEF4444).copy(alpha = 0.2f), radius = 24f, center = pinPos)
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("Lekki Phase 1, Lagos", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
                }
            }
        }

        // Contact inquiry input form card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Send A Feedback Message", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                if (!isSubmitted) {
                    OutlinedTextField(
                        value = nameState,
                        onValueChange = { nameState = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth().testTag("contact_fullname"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = emailState,
                        onValueChange = { emailState = it },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth().testTag("contact_email"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = subjectState,
                        onValueChange = { subjectState = it },
                        label = { Text("Subject") },
                        modifier = Modifier.fillMaxWidth().testTag("contact_subject"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = msgState,
                        onValueChange = { msgState = it },
                        label = { Text("Message Body") },
                        modifier = Modifier.fillMaxWidth().height(90.dp).testTag("contact_message"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Button(
                        onClick = {
                            if (nameState.isNotEmpty() && emailState.contains("@") && msgState.isNotEmpty()) {
                                viewModel.submitContact(nameState, emailState, subjectState, msgState) {
                                    isSubmitted = true
                                    nameState = ""
                                    emailState = ""
                                    subjectState = ""
                                    msgState = ""
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("contact_submit_btn"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Send Message")
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFDCFCE7), RoundedCornerShape(8.dp))
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.CheckCircle, "Done", tint = Color(0xFF15803D))
                            Text("Message Transmitted!", fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                            Text("Praise Tech support will reply to your inbox soon.", fontSize = 11.sp, color = Color(0xFF166534))
                            TextButton(onClick = { isSubmitted = false }) {
                                Text("Send another message")
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. REGISTRATION & LOGIN SYSTEMS
// ==========================================

@Composable
fun AuthScreen(viewModel: LmsViewModel) {
    var isRegisterState by remember { mutableStateOf(false) }

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }

    var errorMsg by remember { mutableStateOf("") }
    val localCtx = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.School,
            contentDescription = "Logo",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )

        Text(
            text = if (isRegisterState) "Create Free Account" else "Welcome back to Praise Tech",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Text(
            text = if (isRegisterState) "Unlock full course catalogs, free lectures, and automated graduation badges immediately." else "Log in to reference your enrolled paths, visual playback positions, and issue reports.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        if (errorMsg.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .background(Color(0xFFFEE2E2), RoundedCornerShape(8.dp))
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(text = errorMsg, color = Color(0xFF991B1B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Input forms matching parameters exactly
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isRegisterState) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth().testTag("reg_fullname"),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    modifier = Modifier.fillMaxWidth().testTag("reg_email"),
                    shape = RoundedCornerShape(8.dp)
                )

                if (isRegisterState) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth().testTag("reg_phone"),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("reg_password"),
                    shape = RoundedCornerShape(8.dp)
                )

                if (isRegisterState) {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("reg_confirmpassword"),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                if (!isRegisterState) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it })
                            Text("Remember Me", fontSize = 12.sp)
                        }

                        TextButton(onClick = { Toast.makeText(localCtx, "Instructions sent to registered mailbox if exists.", Toast.LENGTH_LONG).show() }) {
                            Text("Forgot Password?", fontSize = 12.sp)
                        }
                    }
                }

                Button(
                    onClick = {
                        errorMsg = ""
                        if (isRegisterState) {
                            if (fullName.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
                                errorMsg = "Complete all fields first."
                                return@Button
                            }
                            if (password != confirmPassword) {
                                errorMsg = "Passwords do not match."
                                return@Button
                            }
                            viewModel.registerStudent(fullName, email, phone, password, {
                                Toast.makeText(localCtx, "Sign Up Securely Complete!", Toast.LENGTH_SHORT).show()
                                viewModel.navigateTo("dashboard")
                            }, {
                                errorMsg = it
                            })
                        } else {
                            if (email.isEmpty() || password.isEmpty()) {
                                errorMsg = "Enter valid authentication credentials."
                                return@Button
                            }
                            viewModel.loginUser(email, password, { role ->
                                Toast.makeText(localCtx, "Authenticated Successfully as $role!", Toast.LENGTH_SHORT).show()
                                if (role == "admin") {
                                    viewModel.navigateTo("admin_dashboard")
                                } else {
                                    viewModel.navigateTo("dashboard")
                                }
                            }, {
                                errorMsg = it
                            })
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("auth_action_btn"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isRegisterState) "Create Account" else "Secure Login")
                }
            }
        }

        // Demo login credentials hints
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("📋 Quick Testing Roles (No Sign-Up Needed)", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                Text("• Administrator Creds — Email: admin@praisetech.com | Pass: admin", fontSize = 10.sp)
                Text("• Active Student Creds — Email: student@praisetech.com | Pass: student", fontSize = 10.sp)
            }
        }

        TextButton(onClick = {
            isRegisterState = !isRegisterState
            errorMsg = ""
        }) {
            Text(
                if (isRegisterState) "Already have an account? Sign in"
                else "New to Praise Tech? Register for free account"
            )
        }
    }
}

// ==========================================
// 6. STUDENT DASHBOARD
// ==========================================

@Composable
fun StudentDashboardScreen(viewModel: LmsViewModel) {
    val currentUser by viewModel.currentUserState.collectAsStateWithLifecycle()
    val enrollments by viewModel.userEnrollments.collectAsStateWithLifecycle()
    val payments by viewModel.userPayments.collectAsStateWithLifecycle()
    val certificates by viewModel.userCertificates.collectAsStateWithLifecycle()
    val progressList by viewModel.userProgress.collectAsStateWithLifecycle()
    val courses by viewModel.allCourses.collectAsStateWithLifecycle()

    var activeTabSubState by remember { mutableStateOf("Overview") } // Overview, My Courses, Payments, Settings

    if (currentUser == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Access Denied. Relocate to Authentication screen.")
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Welcome Header Profile Row
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentUser!!.fullName.take(2).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 18.sp
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("Welcome Back,", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                    Text(currentUser!!.fullName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Class Level: Registered Student", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                }
            }
        }

        // Option selection Header pill tabs
        val options = listOf("Overview", "My Courses", "Payments", "Settings")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(options) { o ->
                val selected = activeTabSubState == o
                FilterChip(
                    selected = selected,
                    onClick = { activeTabSubState = o },
                    label = { Text(o, fontSize = 12.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Display contents matching tab
        when (activeTabSubState) {
            "Overview" -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // KPI stats widgets
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatMiniGridItem(title = "Class Paths", count = enrollments.size.toString(), icon = Icons.Default.MenuBook, modifier = Modifier.weight(1f))
                            StatMiniGridItem(title = "Certificates", count = certificates.size.toString(), icon = Icons.Default.WorkspacePremium, modifier = Modifier.weight(1f))
                            StatMiniGridItem(title = "Expenses", count = "₦${String.format("%,.0f", payments.sumOf { it.amount })}", icon = Icons.Default.CreditCard, modifier = Modifier.weight(1.5f))
                        }
                    }

                    item {
                        Text("Active Progress Portfolio", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    if (enrollments.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("You haven't enrolled yet", fontWeight = FontWeight.SemiBold)
                                    Button(onClick = { viewModel.navigateTo("courses") }) {
                                        Text("Explore Courses Catalog")
                                    }
                                }
                            }
                        }
                    } else {
                        items(enrollments) { enrollment ->
                            val matchedCourse = courses.find { it.id == enrollment.courseId }
                            if (matchedCourse != null) {
                                // Calculate course percent
                                DashboardProgressRow(
                                    course = matchedCourse,
                                    progressList = progressList,
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }

            "My Courses" -> {
                if (enrollments.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.School, "empty", modifier = Modifier.size(48.dp))
                            Text("No courses joined.", fontWeight = FontWeight.Bold)
                            TextButton(onClick = { viewModel.navigateTo("courses") }) {
                                Text("Select a course now")
                            }
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(enrollments) { enrollment ->
                            val matchedCourse = courses.find { it.id == enrollment.courseId }
                            if (matchedCourse != null) {
                                DashboardProgressRow(
                                    course = matchedCourse,
                                    progressList = progressList,
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }

            "Payments" -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Payment Billing Archive", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                    if (payments.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No billing ledger registered. Joining free paths is unbilled.")
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(payments) { p ->
                                val matchedCourse = courses.find { it.id == p.courseId }
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(matchedCourse?.title ?: "Technology Course path", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("Ref: ${p.reference} • ${p.paymentMethod}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("₦${String.format("%,.0f2", p.amount)}", fontWeight = FontWeight.Bold)
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFFDCFCE7), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text(p.status, color = Color(0xFF15803D), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "Settings" -> {
                var newPhone by remember { mutableStateOf(currentUser!!.phoneNumber) }
                var newName by remember { mutableStateOf(currentUser!!.fullName) }
                var isSaved by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Configure Student Profiling Info", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                    OutlinedTextField(
                        value = newName,
                        onValueChange = { 
                            newName = it 
                            isSaved = false
                        },
                        label = { Text("Update Full Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = { 
                            newPhone = it 
                            isSaved = false
                        },
                        label = { Text("Update Phone Number") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            viewModel.logout()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sign Out From Account Session")
                    }
                }
            }
        }
    }
}

@Composable
fun StatMiniGridItem(title: String, count: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text(title, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Text(count, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
        }
    }
}

@Composable
fun DashboardProgressRow(
    course: CourseEntity,
    progressList: List<ProgressEntity>,
    viewModel: LmsViewModel
) {
    var lessonsList by remember { mutableStateOf<List<LessonEntity>>(emptyList()) }
    val currentUser by viewModel.currentUserState.collectAsStateWithLifecycle()
    val coroutine = rememberCoroutineScope()
    var showCertificateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(course.id) {
        // Collect local lessons
        viewModel.repository.getLessonsForCourse(course.id).let {
            lessonsList = it
        }
    }

    val totalLessons = lessonsList.size
    val completedLessonsCount = if (totalLessons == 0) 0 else {
        val lessonIds = lessonsList.map { it.id }
        progressList.filter { it.lessonId in lessonIds && it.isCompleted }.size
    }

    val progressPercent = if (totalLessons == 0) 0f else {
        completedLessonsCount.toFloat() / totalLessons.toFloat()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = course.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("Taught by ${course.instructor}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }

                // Certificate display button if completed!
                if (progressPercent >= 1.0f) {
                    IconButton(onClick = { showCertificateDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = "Certificate Unlocked",
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Visual linear Indicator progress bar design
            LinearProgressIndicator(
                progress = { progressPercent },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Modules completed: ${completedLessonsCount}/${totalLessons} (${(progressPercent * 100).toInt()}%)",
                    fontSize = 11.sp
                )

                Button(
                    onClick = {
                        viewModel.selectedCourse.value = course
                        if (lessonsList.isNotEmpty()) {
                            viewModel.activeLesson.value = lessonsList.first()
                        }
                        viewModel.navigateTo("learning_area")
                    },
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Text("Continue", fontSize = 11.sp)
                }
            }
        }
    }

    // Modal view certificate drawing Canvas dialog
    if (showCertificateDialog && currentUser != null) {
        GraduationCertificateDialog(
            user = currentUser!!,
            course = course,
            onDismiss = { showCertificateDialog = false }
        )
    }
}

// ==========================================
// 7. COURSE LEARNING AREA (VIDEO PLAYER)
// ==========================================

@Composable
fun CourseLearningAreaScreen(viewModel: LmsViewModel) {
    val selectedCourse by viewModel.selectedCourse.collectAsStateWithLifecycle()
    val activeLesson by viewModel.activeLesson.collectAsStateWithLifecycle()
    val userProgress by viewModel.userProgress.collectAsStateWithLifecycle()

    var lessonsList by remember { mutableStateOf<List<LessonEntity>>(emptyList()) }
    val coroutine = rememberCoroutineScope()

    if (selectedCourse == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select a Course from dashboard first.")
        }
        return
    }

    LaunchedEffect(selectedCourse!!.id) {
        viewModel.repository.getLessonsForCourse(selectedCourse!!.id).let {
            lessonsList = it
            if (activeLesson == null && it.isNotEmpty()) {
                viewModel.activeLesson.value = it.first()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Upper Navigation Info Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = { viewModel.navigateTo("dashboard") }) {
                Icon(Icons.Default.ArrowBack, "Back")
            }

            Column {
                Text(selectedCourse!!.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Learning Path Video Engine", fontSize = 10.sp)
            }
        }

        // 1. VIDEO LEARNING COMPONENT CONTROL AREA
        if (activeLesson != null) {
            val lessonProgress = userProgress.find { it.lessonId == activeLesson!!.id }
            VideoSimulatorPlayer(
                lesson = activeLesson!!,
                progress = lessonProgress,
                onProgressUpdateOnPause = { secs ->
                    viewModel.updateLessonProgressSeconds(activeLesson!!, secs)
                },
                onComplete = {
                    viewModel.markLessonCompleted(activeLesson!!)
                }
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color.Black, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("Initializing lectures database list...", color = Color.White)
            }
        }

        // Active Lesson text Details
        if (activeLesson != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = activeLesson!!.lessonTitle, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(text = "Module Node: ${activeLesson!!.moduleTitle}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        HorizontalDivider()

        Text("All Course Lectures", fontWeight = FontWeight.Bold, fontSize = 14.sp)

        // Modules & Lessons playlist navigator
        if (lessonsList.isEmpty()) {
            Text("No Lessons registered.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                val groupLessons = lessonsList.groupBy { it.moduleTitle }
                groupLessons.forEach { (moduleTitle, lessonsInModule) ->
                    item {
                        Text(
                            text = moduleTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    items(lessonsInModule) { l ->
                        val isPlayingThis = activeLesson?.id == l.id
                        val isFinished = userProgress.find { it.lessonId == l.id }?.isCompleted ?: false

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.activeLesson.value = l },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isPlayingThis) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            border = if (isPlayingThis) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isFinished) Icons.Default.CheckCircle
                                    else if (isPlayingThis) Icons.Default.PlayCircleFilled
                                    else Icons.Default.PlayCircleOutline,
                                    contentDescription = "Status",
                                    tint = if (isFinished) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = l.lessonTitle,
                                        fontWeight = if (isPlayingThis) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                    Text(text = "Duration: ${l.videoDurationSeconds} seconds", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Graphic canvas-drawn video visual playback mock player
@Composable
fun VideoSimulatorPlayer(
    lesson: LessonEntity,
    progress: ProgressEntity?,
    onProgressUpdateOnPause: (Int) -> Unit,
    onComplete: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(false) }
    var speedMultiplier by remember { mutableStateOf(1f) }
    var currentProgressSeconds by remember(lesson.id) {
        mutableStateOf(progress?.lastPositionSeconds ?: 0)
    }
    var isFullScreen by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Control ticking visualizer loop
    LaunchedEffect(isPlaying, speedMultiplier, lesson.id) {
        if (isPlaying) {
            while (currentProgressSeconds < lesson.videoDurationSeconds) {
                delay((1000 / speedMultiplier).toLong())
                currentProgressSeconds += 1
                onProgressUpdateOnPause(currentProgressSeconds)
            }
            // Loop ended, complete triggers
            isPlaying = false
            onComplete()
        }
    }

    val playPct = currentProgressSeconds.toFloat() / lesson.videoDurationSeconds.toFloat()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Draw visual Canvas to resemble high-fidelity active video layout
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isFullScreen) 280.dp else 170.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Background visual wave representation
                val centerH = size.height / 2
                val wavePath = Path().apply {
                    moveTo(0f, centerH)
                    for (x in 0..size.width.toInt() step 20) {
                        val activeCoeff = if (isPlaying) 15f else 5f
                        val waveY = centerH + kotlin.math.sin((x + currentProgressSeconds * 10f) * 0.05f) * activeCoeff
                        lineTo(x.toFloat(), waveY)
                    }
                }
                drawPath(wavePath, Color(0xFF0EA5E9).copy(alpha = 0.4f), style = Stroke(width = 4f))

                // Playback progress circle overlay
                val activeRadius = size.width / 5
                drawCircle(
                    color = Color.White.copy(alpha = 0.05f),
                    radius = activeRadius,
                    center = Offset(size.width / 2, size.height / 2)
                )
            }

            // Central Visual Play state
            IconButton(
                onClick = { isPlaying = !isPlaying },
                modifier = Modifier
                    .size(52.dp)
                    .background(Color.White.copy(alpha = 0.85f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Trigger Play",
                    tint = Color.Black,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Overlay text details
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("Lecturer: Praise Solution", color = Color.White, fontSize = 9.sp)
            }
        }

        // Scrub slider timeline
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${currentProgressSeconds / 60}:${String.format("%02d", currentProgressSeconds % 60)}",
                    color = Color.White,
                    fontSize = 11.sp
                )

                Text(
                    text = "${lesson.videoDurationSeconds / 60}:${String.format("%02d", lesson.videoDurationSeconds % 60)}",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }

            Slider(
                value = playPct,
                onValueChange = { newVal ->
                    currentProgressSeconds = (newVal * lesson.videoDurationSeconds).toInt()
                    onProgressUpdateOnPause(currentProgressSeconds)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    activeTrackColor = Color(0xFF0EA5E9),
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f),
                    thumbColor = Color.White
                )
            )
        }

        // Layout controls row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Speed controller
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Speed: ", color = Color.White, fontSize = 10.sp)
                val ratePills = listOf(1f, 1.5f, 2f)
                ratePills.forEach { rate ->
                    val isSelected = rate == speedMultiplier
                    Box(
                        modifier = Modifier
                            .background(
                                if (isSelected) Color(0xFF0EA5E9) else Color.White.copy(alpha = 0.1f),
                                RoundedCornerShape(4.dp)
                            )
                            .clickable { speedMultiplier = rate }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("${rate}x", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Fullscreen trigger and manual completion toggle
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { isFullScreen = !isFullScreen }) {
                    Icon(
                        imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = "Fullscreen Toggle",
                        tint = Color.White
                    )
                }

                Button(
                    onClick = onComplete,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.DoneAll, null, tint = Color.White, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Complete", fontSize = 10.sp, color = Color.White)
                }
            }
        }
    }
}

// ==========================================
// 8. CANVAS-BASED GRADUATION CERTIFICATES
// ==========================================

@Composable
fun GraduationCertificateDialog(
    user: UserEntity,
    course: CourseEntity,
    onDismiss: () -> Unit
) {
    val localCtx = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2C59)) // Elegant deep corporate navy board
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("graduation file success", color = Color(0xFFFBBF24), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // Interactive Certificate Board Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(290.dp)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                        .border(BorderStroke(2.dp, Color(0xFF0F2C59)), RoundedCornerShape(6.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Drawing corner elements representing high class frames
                        val margin = 10f
                        val framePath = Path().apply {
                            moveTo(margin, margin + 40f)
                            lineTo(margin, margin)
                            lineTo(margin + 40f, margin)

                            moveTo(size.width - margin - 40f, margin)
                            lineTo(size.width - margin, margin)
                            lineTo(size.width - margin, margin + 40f)

                            moveTo(size.width - margin, size.height - margin - 40f)
                            lineTo(size.width - margin, size.height - margin)
                            lineTo(size.width - margin - 40f, size.height - margin)

                            moveTo(margin + 40f, size.height - margin)
                            lineTo(margin, size.height - margin)
                            lineTo(margin, size.height - margin - 40f)
                        }
                        drawPath(framePath, Color(0xFFD4AF37), style = Stroke(width = 3f)) // Gold color borders

                        // Draw seal emblem circle
                        drawCircle(
                            color = Color(0xFF1E40AF),
                            radius = 24f,
                            center = Offset(size.width * 0.85f, size.height * 0.8f)
                        )
                    }

                    // Floating details
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Praise Tech Solution",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F2C59)
                        )
                        Text(
                            text = "CERTIFICATE OF TECH GRADUATION",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp,
                            color = Color(0xFF475569)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "This is proudly awarded to",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )

                        Text(
                            text = user.fullName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F2C59),
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "for outstanding study performance in completing the standard technical training curriculum for",
                            fontSize = 9.sp,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF334155),
                            lineHeight = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Text(
                            text = course.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E40AF),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Praise Victor", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                HorizontalDivider(modifier = Modifier.width(70.dp), color = Color.Black)
                                Text("Chief Chancellor", fontSize = 8.sp, color = Color.Gray)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Code: PTS-${course.id}-9421", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Text("Date: 2026-06-03", fontSize = 8.sp, color = Color.Gray)
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        Toast.makeText(localCtx, "Exporting and rendering branded Certificate PDF to Downloads... Success!", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBBF24), contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Download, "Download")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Download PDF Certificate", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// 9. ADMIN PANEL DASHBOARD CONSOLE
// ==========================================

@Composable
fun AdminDashboardScreen(viewModel: LmsViewModel) {
    val students by viewModel.allStudents.collectAsStateWithLifecycle()
    val courses by viewModel.allCourses.collectAsStateWithLifecycle()
    val payments by viewModel.paymentsList.collectAsStateWithLifecycle()
    val contacts by viewModel.contactsList.collectAsStateWithLifecycle()

    var activeAdminTab by remember { mutableStateOf("Overview") } // Overview, Courses, Students, Payments, Inbox

    // State parameters for adding new courses
    var showAddCourseDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Welcoming header title
        Text("Praise Tech Administration Hub", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

        val adminTabs = listOf("Overview", "Courses", "Students", "Payments", "Inbox")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(adminTabs) { tab ->
                val selected = activeAdminTab == tab
                FilterChip(
                    selected = selected,
                    onClick = { activeAdminTab = tab },
                    label = { Text(tab, fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Tab routing implementation
        when (activeAdminTab) {
            "Overview" -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Statistics KPI metrics cards
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AdminKpiGridCard(title = "Total Students", count = students.size.toString(), icon = Icons.Default.Groups, modifier = Modifier.weight(1f))
                                AdminKpiGridCard(title = "Total Courses", count = courses.size.toString(), icon = Icons.Default.LibraryBooks, modifier = Modifier.weight(1f))
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AdminKpiGridCard(title = "Total Revenue", count = "₦${String.format("%,.0f", payments.sumOf { it.amount })}", icon = Icons.Default.Savings, modifier = Modifier.weight(1f))
                                AdminKpiGridCard(title = "Total Payments", count = payments.size.toString(), icon = Icons.Default.VerifiedUser, modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    item {
                        Text("Instant Activity Index", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("• Admin Account Access: ACTIVE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("• SQLite Room Persistence Tables Status: ONLINE", fontSize = 11.sp)
                                Text("• Total Certificates Auth: ${payments.filter { it.status == "Success" }.size} Potential", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            "Courses" -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active Courses List", fontWeight = FontWeight.Bold)
                        Button(
                            onClick = { showAddCourseDialog = true },
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Add, "Add", modifier = Modifier.size(14.dp))
                            Text("New Course", fontSize = 11.sp)
                        }
                    }

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(courses) { c ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(c.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Inst: ${c.instructor} | ${c.category}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                        Text(if (c.isPaid) "Price: ₦${String.format("%,.0f", c.price)}" else "FREE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    IconButton(onClick = { viewModel.deleteCourseAdmin(c) }) {
                                        Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "Students" -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Registered Student Profiles List", fontWeight = FontWeight.Bold)

                    if (students.isEmpty()) {
                        Text("No registered students found.", fontSize = 12.sp)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(students) { s ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(s.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Email: ${s.email}", fontSize = 11.sp)
                                        Text("Phone: ${s.phoneNumber}", fontSize = 11.sp)
                                        Text("Role Node: ${s.role.uppercase()}", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "Payments" -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Payment Audit Ledger", fontWeight = FontWeight.Bold)

                    if (payments.isEmpty()) {
                        Text("No payments indexed yet.", fontSize = 12.sp)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(payments) { p ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Tx Ref: ${p.reference}", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            Text("UserId: ${p.userId} • Gateway: Flutterwave", fontSize = 11.sp)
                                            Text("Method: ${p.paymentMethod}", fontSize = 11.sp)
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("₦${String.format("%,.0f", p.amount)}", fontWeight = FontWeight.Bold)
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFFDCFCE7), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(p.status, color = Color(0xFF15803D), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "Inbox" -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Support Inbox Messages", fontWeight = FontWeight.Bold)

                    if (contacts.isEmpty()) {
                        Text("No mailbox messages registered.", fontSize = 12.sp)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(contacts) { m ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Sender: ${m.senderName} (${m.senderEmail})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("Subject: ${m.subject}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                        Text("Message: ${m.message}", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal view dialog to create new course
    if (showAddCourseDialog) {
        AddCourseAdminDialog(
            viewModel = viewModel,
            onDismiss = { showAddCourseDialog = false }
        )
    }
}

@Composable
fun AdminKpiGridCard(title: String, count: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }

            Column {
                Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Text(count, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun AddCourseAdminDialog(
    viewModel: LmsViewModel,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var inst by remember { mutableStateOf("Mrs. Praise Jenkins") }
    var dur by remember { mutableStateOf("10 Hours") }
    var priceStr by remember { mutableStateOf("15000") }
    var category by remember { mutableStateOf("Software Engineering") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Create Course Profile", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Course Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Course Description") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = inst, onValueChange = { inst = it }, label = { Text("Instructor Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = dur, onValueChange = { dur = it }, label = { Text("Duration (e.g. 15 Hours)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = priceStr, onValueChange = { priceStr = it }, label = { Text("Price (0 for Free)") }, modifier = Modifier.fillMaxWidth())

                Button(
                    onClick = {
                        val p = priceStr.toDoubleOrNull() ?: 0.0
                        if (title.isNotEmpty() && desc.isNotEmpty()) {
                            viewModel.addCourse(title, desc, inst, dur, p, category)
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Insert & Activate Course")
                }
            }
        }
    }
}

// ==========================================
// 10. FLUTTERWAVE GATEWAY SIMULATION INTERACTIVE MODAL
// ==========================================

@Composable
fun FlutterwaveCheckoutDialog(
    course: CourseEntity,
    viewModel: LmsViewModel,
    onDismiss: () -> Unit
) {
    var checkTabState by remember { mutableStateOf("Card") } // Card, Bank, USSD
    val statusText by viewModel.paymentStatusMessage.collectAsStateWithLifecycle()

    var cardNo by remember { mutableStateOf("4000 1234 5678 9010") }
    var expiry by remember { mutableStateOf("12/29") }
    var cvv by remember { mutableStateOf("123") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("flutterwave SECURE CHECKOUT", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp, fontSize = 10.sp, color = Color(0xFFEF4444))
                        Text(course.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider()

                // Display active price tag
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFEF2F2), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total Amount Due:", fontWeight = FontWeight.Bold)
                    Text("₦${String.format("%,.2f", course.price)}", fontWeight = FontWeight.ExtraBold, color = Color(0xFFEF4444), fontSize = 16.sp)
                }

                // Selector for Flutterwave options
                val payTypes = listOf("Card", "Bank Transfer", "USSD")
                TabRow(
                    selectedTabIndex = payTypes.indexOf(checkTabState),
                    containerColor = Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    payTypes.forEachIndexed { idx, type ->
                        Tab(
                            selected = checkTabState == type,
                            onClick = { checkTabState = type },
                            text = { Text(type, fontSize = 12.sp) }
                        )
                    }
                }

                // Interactive Payment fields based on tab selection
                when (checkTabState) {
                    "Card" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = cardNo,
                                onValueChange = { cardNo = it },
                                label = { Text("Debit/Credit Card Number") },
                                leadingIcon = { Icon(Icons.Default.CreditCard, null) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = expiry,
                                    onValueChange = { expiry = it },
                                    label = { Text("Expiry (MM/YY)") },
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = cvv,
                                    onValueChange = { cvv = it },
                                    label = { Text("CVV Security") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    "Bank Transfer" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Praise Tech Solution Billing Node Account:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text("Bank: Flutterwave Microfinance Bank", fontSize = 11.sp)
                                Text("Account Number: 9382012831", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Please proceed to make a transfer to the exact billing account. Enrolling updates automatically.", fontSize = 10.sp)
                            }
                        }
                    }

                    "USSD" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFFBEB), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Selected Bank Standard USSD Code:", fontSize = 11.sp)
                                Text("*322*8*15000#", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                                Text("Dial the security code directly inside your handset device to authorize payment.", fontSize = 10.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }

                // Dynamic live state status
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(statusText, fontSize = 11.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, textAlign = TextAlign.Center)
                }

                // Check authorization execution
                Button(
                    onClick = {
                        viewModel.executeSimulatedPayment(checkTabState)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("confirm_payment_btn")
                ) {
                    Icon(Icons.Default.Lock, "Lock", tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Authorize Secure Payment", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
