package kr.sunsetcoast.config.scheduling

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

/** 정해진 시각에 도는 작업 (로그인 기록 보관 기간 정리 등) */
@Configuration
@EnableScheduling
class SchedulingConfiguration
