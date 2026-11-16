package kr.sunsetcoast.domain.bank.service

import kr.sunsetcoast.domain.bank.repository.BankCategoryRuleRepository
import kr.sunsetcoast.domain.bank.vo.BankCategorizer
import kr.sunsetcoast.domain.businessowner.repository.BusinessOwnerRepository
import kr.sunsetcoast.domain.businessowner.vo.OwnershipType
import org.springframework.stereotype.Component

/** 관리자 규칙(긴 키워드 먼저)과 관계사 이름으로 분류기를 만든다 */
@Component
class BankCategorizerFactory(
    private val ruleRepository: BankCategoryRuleRepository,
    private val businessOwnerRepository: BusinessOwnerRepository,
) {
    fun create(): BankCategorizer {
        val rules = ruleRepository.findAll().sortedByDescending { it.keyword.length }.map { it.toRule() }
        val affiliates = businessOwnerRepository.findAll()
            .filter { it.ownershipType == OwnershipType.FAMILY }
            .map { it.name.replace("(주)", "") }
        return BankCategorizer(rules, affiliates)
    }
}
