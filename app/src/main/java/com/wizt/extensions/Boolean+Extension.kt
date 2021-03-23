package matteocrippa.it.karamba


fun Boolean.toggle(): Boolean {
    return !this
}

fun Boolean.random(): Boolean {
    return (0..1).random() as Boolean
}