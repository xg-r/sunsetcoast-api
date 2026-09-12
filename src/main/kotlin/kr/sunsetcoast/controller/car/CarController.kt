package kr.sunsetcoast.controller.car

import kr.sunsetcoast.controller.car.dto.CarResponseDto
import kr.sunsetcoast.domain.car.service.CarService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/cars")
class CarController(
    val carService: CarService,
) {
    @GetMapping(version = "1.0")
    fun getCars(): List<CarResponseDto> = carService.getCars()
        .map { CarResponseDto.from(it) }
}
