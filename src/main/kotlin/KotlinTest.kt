class KotlinTest {

}

interface Yes {
    val yes: String
    val no: String? get() = null
}

val d = object : Yes {
    override val yes = "Hello"
    override val no: String? = null
}
