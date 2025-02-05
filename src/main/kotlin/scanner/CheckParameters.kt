package scanner

import burp.api.montoya.MontoyaApi
import burp.api.montoya.http.message.HttpRequestResponse
import burp.api.montoya.scanner.AuditResult
import burp.api.montoya.scanner.ConsolidationAction
import burp.api.montoya.scanner.ScanCheck
import burp.api.montoya.scanner.ScanTask
import burp.api.montoya.scanner.Scanner
import burp.api.montoya.scanner.audit.insertionpoint.AuditInsertionPoint
import burp.api.montoya.scanner.audit.issues.AuditIssue
import controller.HttpInterceptor
import model.logentry.LogEntry
import model.logentry.ModifiedLogEntry
import  burp.api.montoya.scanner.ConsolidationAction.KEEP_BOTH;
import  burp.api.montoya.scanner.ConsolidationAction.KEEP_EXISTING;

class CheckParameters(private val logs: LogEntry,
                      private val api: MontoyaApi,
                      private val modifiedLog: ModifiedLogEntry
) : ScanCheck {
    override fun activeAudit(
        baseRequestResponse: HttpRequestResponse?,
        auditInsertionPoint: AuditInsertionPoint?
    ): AuditResult? {
        return null
    }

    override fun passiveAudit(baseRequestResponse: HttpRequestResponse): AuditResult? {
        HttpInterceptor(logs,api,modifiedLog).processHttpResponse(baseRequestResponse)
        println(baseRequestResponse.request().url())
        return null
    }

    override fun consolidateIssues(
        newIssue: AuditIssue?,
        existingIssue: AuditIssue?
    ): ConsolidationAction? {
        return null

    }

}