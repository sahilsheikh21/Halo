import org.matrix.rustcomponents.sdk.Room
import org.matrix.rustcomponents.sdk.RoomMember

fun main() {
    println(Room::class.java.methods.joinToString("\n") { it.name })
    println("=====")
    println(RoomMember::class.java.methods.joinToString("\n") { it.name })
}
