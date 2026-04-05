package com.halo.matrix
import org.junit.Test
import org.matrix.rustcomponents.sdk.*
class ReflectionTest {
    @Test
    fun dumpApi() {
        ClientBuilder::class.java.methods.forEach { println("ClientBuilder." + it.name) }
        Session::class.java.methods.forEach { println("Session." + it.name) }
        Client::class.java.methods.forEach { println("Client." + it.name) }
    }
}
