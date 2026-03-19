package kr.sunsetcoast.controller.room

import kr.sunsetcoast.domain.room.service.RoomService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class RoomController(
    private val roomService: RoomService,
) {
    @GetMapping("/rooms")
    fun getRooms(@RequestParam size: Int, @RequestParam page: Int): String {
        roomService.findRooms(size = size, page = page)
        return "OK"
    }
}
