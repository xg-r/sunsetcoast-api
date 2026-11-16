package kr.sunsetcoast.controller.importjob

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import kr.sunsetcoast.config.security.CurrentUser
import kr.sunsetcoast.domain.importjob.service.ImportJobService
import kr.sunsetcoast.domain.importjob.service.ImportJobWorker
import kr.sunsetcoast.domain.importjob.vo.ImportJobKind
import kr.sunsetcoast.domain.user.vo.UserRole
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import tools.jackson.databind.json.JsonMapper

@Tag(name = "Import Job", description = "파일 업로드 비동기 처리")
interface ImportJobControllerDocs {
    @Operation(
        summary = "정산 파일 업로드 (비동기)",
        description = "파일마다 작업을 만들고 바로 202로 응답한다. 백그라운드에서 한 번에 하나씩 처리하고, 실패하면 올린 사용자에게 알림을 남긴다. " +
            "미리보기는 기존 POST /settlements/import?dryRun=true (바로 결과).",
    )
    fun enqueueSettlements(
        @Parameter(content = [Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, array = ArraySchema(schema = Schema(type = "string", format = "binary")))])
        files: List<MultipartFile>,
    ): ImportJobsCreatedResponseBody

    @Operation(
        summary = "계좌 거래내역 업로드 (비동기, 슈퍼어드민)",
        description = "정산 파일 업로드와 같은 방식. 어느 사업자의 계좌인지 businessOwnerId로 고른다 " +
            "(파일에 계좌번호 줄이 없을 수 있음). 파일의 계좌가 다른 사업자 것이면 작업이 실패한다.",
    )
    fun enqueueBank(
        @Parameter(description = "사업자 ID", example = "3") businessOwnerId: Long,
        @Parameter(content = [Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, array = ArraySchema(schema = Schema(type = "string", format = "binary")))])
        files: List<MultipartFile>,
    ): ImportJobsCreatedResponseBody

    @Operation(
        summary = "업로드 작업 상태",
        description = "ids를 주면 그 작업들(진행 확인용), 없으면 최근 50개. 슈퍼어드민이 아니면 자기가 올린 작업만 보인다.",
    )
    fun getJobs(@Parameter(description = "작업 ID 목록", example = "1,2") ids: List<Long>?): List<ImportJobResponseBody>
}

@RestController
class ImportJobController(
    private val jobService: ImportJobService,
    private val worker: ImportJobWorker,
    private val jsonMapper: JsonMapper,
) : ImportJobControllerDocs {
    @PostMapping("/settlements/import-jobs", version = "1.0", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.ACCEPTED)
    override fun enqueueSettlements(@RequestPart("files") files: List<MultipartFile>) = enqueue(ImportJobKind.SETTLEMENT, files)

    @PostMapping("/bank/import-jobs", version = "1.0", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.ACCEPTED)
    override fun enqueueBank(
        @RequestParam businessOwnerId: Long,
        @RequestPart("files") files: List<MultipartFile>,
    ) = enqueue(ImportJobKind.BANK, files, businessOwnerId)

    @GetMapping("/import-jobs", version = "1.0")
    override fun getJobs(@RequestParam(required = false) ids: List<Long>?): List<ImportJobResponseBody> {
        val user = CurrentUser.get()
        val canSeeAll = user?.hasRole(UserRole.SUPER_ADMIN) == true
        val jobs = if (ids.isNullOrEmpty()) jobService.recent(user?.userId, canSeeAll) else jobService.find(ids, user?.userId, canSeeAll)
        return jobs.map { ImportJobResponseBody.from(it, jsonMapper) }
    }

    private fun enqueue(kind: ImportJobKind, files: List<MultipartFile>, businessOwnerId: Long? = null): ImportJobsCreatedResponseBody {
        require(files.isNotEmpty()) { "파일을 골라 주세요." }
        val created = jobService.create(kind, files.map { (it.originalFilename ?: "unknown") to it.bytes }, CurrentUser.get()?.userId, businessOwnerId)
        // create의 트랜잭션이 끝난 뒤에 넘겨야 작업자가 저장된 작업을 읽는다
        worker.submit(created.mapNotNull { it.job?.id })
        return ImportJobsCreatedResponseBody.from(created, jsonMapper)
    }
}
