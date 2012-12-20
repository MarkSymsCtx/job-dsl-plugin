package javaposse.jobdsl.dsl.helpers

import javaposse.jobdsl.dsl.WithXmlAction

import javaposse.jobdsl.dsl.helpers.PublisherContextHelper.PublisherContext
import spock.lang.Specification
import javax.xml.stream.events.NotationDeclaration

public class PublisherHelperSpec extends Specification {

    List<WithXmlAction> mockActions = Mock()
    PublisherContextHelper helper = new PublisherContextHelper(mockActions)
    PublisherContext context = new PublisherContext()

    def 'empty call extended email method'() {
        when:
        context.extendedEmail()

        then:
        context.publisherNodes != null
        context.publisherNodes.size() == 1
        Node emailPublisher = context.publisherNodes[0]
        emailPublisher.name() == 'hudson.plugins.emailext.ExtendedEmailPublisher'
        emailPublisher.recipientList[0].value() == '$DEFAULT_RECIPIENTS'
        emailPublisher.defaultSubject[0].value() == '$DEFAULT_SUBJECT'
        emailPublisher.contentType[0].value() == 'default'
        Node triggers = emailPublisher.configuredTriggers[0]
        triggers.children().size() == 2
        Node email = triggers.children()[0].email[0]
        email.recipientList[0].value() == ''
        email.subject[0].value() == '$PROJECT_DEFAULT_SUBJECT'
        email.body[0].value() == '$PROJECT_DEFAULT_CONTENT'
    }

    def 'call extended email with args'() {
        when:
        context.extendedEmail('me@halfempty.org', 'Oops', 'Something broken') {
            trigger('PreBuild')
            trigger(triggerName: 'StillUnstable', subject: 'Subject', body:'Body', recipientList:'RecipientList',
                    sendToDevelopers: true, sendToRequester: true, includeCulprits: true, sendToRecipientList: false)
            configure { node ->
                node / contentType << 'html'
            }
        }

        then:
        Node emailPublisher = context.publisherNodes[0]
        emailPublisher.recipientList[0].value() == 'me@halfempty.org'
        emailPublisher.defaultSubject[0].value() == 'Oops'
        emailPublisher.defaultContent[0].value() == 'Something broken'
        emailPublisher.contentType.size() == 1
        emailPublisher.contentType[0].value() == 'html'
        Node triggers = emailPublisher.configuredTriggers[0]
        triggers.children().size() == 2
        Node emailDefault = triggers.children()[0].email[0]
        emailDefault.recipientList[0].value() == ''
        emailDefault.subject[0].value() == '$PROJECT_DEFAULT_SUBJECT'
        emailDefault.body[0].value() == '$PROJECT_DEFAULT_CONTENT'
        emailDefault.sendToDevelopers[0].value() as String == 'false'
        emailDefault.sendToRequester[0].value() as String == 'false'
        emailDefault.includeCulprits[0].value() as String == 'false'
        emailDefault.sendToRecipientList[0].value() as String == 'true'

        triggers.children()[1].name() == 'hudson.plugins.emailext.plugins.trigger.StillUnstableTrigger'
        Node email = triggers.children()[1].email[0]
        email.recipientList[0].value() == 'RecipientList'
        email.subject[0].value() == 'Subject'
        email.body[0].value() == 'Body'
        email.sendToDevelopers[0].value() as String == 'true'
        email.sendToRequester[0].value() as String == 'true'
        email.includeCulprits[0].value() as String == 'true'
        email.sendToRecipientList[0].value() as String == 'false'
    }

    def 'call archive artifacts with all args'() {
        when:
        context.archiveArtifacts('include/*', 'exclude/*', true)

        then:
        Node archiveNode = context.publisherNodes[0]
        archiveNode.name() == 'hudson.tasks.ArtifactArchiver'
        archiveNode.artifacts[0].value() == 'include/*'
        archiveNode.excludes[0].value() == 'exclude/*'
        archiveNode.latestOnly[0].value() == 'true'

    }

    def 'call archive artifacts least args'() {
        when:
        context.archiveArtifacts('include/*')

        then:
        Node archiveNode = context.publisherNodes[0]
        archiveNode.name() == 'hudson.tasks.ArtifactArchiver'
        archiveNode.artifacts[0].value() == 'include/*'
        archiveNode.excludes.isEmpty()
        archiveNode.latestOnly[0].value() == 'false'

    }

    def 'calling minimal html publisher'() {
        when:
        context.publishHtml {
            report 'build/*'
        }

        then:
        Node publisherHtmlNode = context.publisherNodes[0]
        publisherHtmlNode.name() == 'htmlpublisher.HtmlPublisher'
        !publisherHtmlNode.reportTargets.isEmpty()
        def target = publisherHtmlNode.reportTargets[0].'htmlpublisher.HtmlPublisherTarget'[0]
        target.reportName[0].value() == ''
        target.reportDir[0].value() == 'build/*'
        target.reportFiles[0].value() == 'index.html'
        target.keepAll[0].value() == 'false'
        target.wrapperName[0].value() == 'htmlpublisher-wrapper.html'
    }

    def 'calling html publisher with a few args'() {
        when:
        context.publishHtml {
            report reportName: 'Report Name', reportDir: 'build/*', reportFiles: 'content.html', keepAll: true
        }

        then:
        Node publisherHtmlNode = context.publisherNodes[0]
        publisherHtmlNode.name() == 'htmlpublisher.HtmlPublisher'
        !publisherHtmlNode.reportTargets.isEmpty()
        def target = publisherHtmlNode.reportTargets[0].'htmlpublisher.HtmlPublisherTarget'[0]
        target.reportName[0].value() == 'Report Name'
        target.reportDir[0].value() == 'build/*'
        target.reportFiles[0].value() == 'content.html'
        target.keepAll[0].value() == 'true'
        target.wrapperName[0].value() == 'htmlpublisher-wrapper.html'
    }

    def 'calling html publisher with map syntax without all args'() {
        when:
        context.publishHtml {
            report reportName: 'Report Name', reportDir: 'build/*'
        }

        then:
        Node publisherHtmlNode = context.publisherNodes[0]
        publisherHtmlNode.name() == 'htmlpublisher.HtmlPublisher'
        !publisherHtmlNode.reportTargets.isEmpty()
        def target = publisherHtmlNode.reportTargets[0].'htmlpublisher.HtmlPublisherTarget'[0]
        target.reportName[0].value() == 'Report Name'
        target.reportDir[0].value() == 'build/*'
        target.reportFiles[0].value() == 'index.html'
        target.keepAll[0].value() == 'false'
        target.wrapperName[0].value() == 'htmlpublisher-wrapper.html'
    }

    def 'calling html publisher with multiple reports'() {
        when:
        context.publishHtml {
            report('build/*', 'Build Report')
            report('test/*', 'Test Report')
        }

        then:
        Node publisherHtmlNode = context.publisherNodes[0]
        publisherHtmlNode.name() == 'htmlpublisher.HtmlPublisher'
        !publisherHtmlNode.reportTargets.isEmpty()
        def target1 = publisherHtmlNode.reportTargets[0].'htmlpublisher.HtmlPublisherTarget'[0]
        target1.reportName[0].value() == 'Build Report'
        target1.reportDir[0].value() == 'build/*'

        def target2 = publisherHtmlNode.reportTargets[0].'htmlpublisher.HtmlPublisherTarget'[1]
        target2.reportName[0].value() == 'Test Report'
        target2.reportDir[0].value() == 'test/*'
    }

    def 'call step via helper'() {
        when:
        helper.publishers {
            extendedEmail()
        }

        then:
        1 * mockActions.add(_)
    }
}
