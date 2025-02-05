//package controller
//
//import burp.api.montoya.core.Annotations
//import burp.api.montoya.core.Registration
//import burp.api.montoya.http.message.HttpHeader
//import burp.api.montoya.http.message.HttpRequestResponse
//import burp.api.montoya.http.message.requests.HttpRequest
//import burp.api.montoya.proxy.MessageReceivedAction
//import burp.api.montoya.proxy.Proxy
//import burp.api.montoya.proxy.ProxyHistoryFilter
//import burp.api.montoya.proxy.ProxyHttpRequestResponse
//import burp.api.montoya.proxy.ProxyWebSocketHistoryFilter
//import burp.api.montoya.proxy.ProxyWebSocketMessage
//import burp.api.montoya.proxy.http.InterceptedRequest
//import burp.api.montoya.proxy.http.InterceptedResponse
//import burp.api.montoya.proxy.http.ProxyRequestHandler
//import burp.api.montoya.proxy.http.ProxyRequestReceivedAction
//import burp.api.montoya.proxy.http.ProxyRequestToBeSentAction
//import burp.api.montoya.proxy.http.ProxyResponseHandler
//import burp.api.montoya.proxy.http.ProxyResponseReceivedAction
//import burp.api.montoya.proxy.http.ProxyResponseToBeSentAction
//import burp.api.montoya.proxy.websocket.ProxyWebSocketCreationHandler
//import burp.api.montoya.scanner.AuditConfiguration
//import burp.api.montoya.scanner.Crawl
//import burp.api.montoya.scanner.CrawlConfiguration
//import burp.api.montoya.scanner.ReportFormat
//import burp.api.montoya.scanner.ScanCheck
//import burp.api.montoya.scanner.Scanner
//import burp.api.montoya.scanner.audit.Audit
//import burp.api.montoya.scanner.audit.AuditIssueHandler
//import burp.api.montoya.scanner.audit.insertionpoint.AuditInsertionPointProvider
//import burp.api.montoya.scanner.audit.issues.AuditIssue
//import burp.api.montoya.scanner.bchecks.BChecks
//import java.nio.file.Path
//
//class HtDe:  ProxyResponseHandler {
//
//
//}