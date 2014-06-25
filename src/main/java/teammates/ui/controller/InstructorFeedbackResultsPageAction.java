package teammates.ui.controller;

import java.util.Iterator;

import teammates.common.datatransfer.FeedbackResponseAttributes;
import teammates.common.datatransfer.FeedbackSessionAttributes;
import teammates.common.datatransfer.InstructorAttributes;
import teammates.common.exception.EntityDoesNotExistException;
import teammates.common.util.Assumption;
import teammates.common.util.Const;
import teammates.logic.api.GateKeeper;

public class InstructorFeedbackResultsPageAction extends Action {

    private static final String ALL_SECTION_OPTION = "All";

    @Override
    protected ActionResult execute() throws EntityDoesNotExistException {

        String courseId = getRequestParamValue(Const.ParamsNames.COURSE_ID);
        String feedbackSessionName = getRequestParamValue(Const.ParamsNames.FEEDBACK_SESSION_NAME);
        Assumption.assertNotNull(courseId);
        Assumption.assertNotNull(feedbackSessionName);

        statusToAdmin = "Show instructor feedback result page<br>" +
                "Session Name: " + feedbackSessionName + "<br>" +
                "Course ID: " + courseId;

        InstructorAttributes instructor = logic.getInstructorForGoogleId(
                courseId, account.googleId);
        FeedbackSessionAttributes session = logic.getFeedbackSession(
                feedbackSessionName, courseId);
        boolean isCreatorOnly = true;

        new GateKeeper().verifyAccessible(instructor, session, !isCreatorOnly);

        InstructorFeedbackResultsPageData data = new InstructorFeedbackResultsPageData(
                account);
        data.selectedSection = getRequestParamValue(Const.ParamsNames.FEEDBACK_RESULTS_GROUPBYSECTION);
        if (data.selectedSection == null) {
            data.selectedSection = ALL_SECTION_OPTION;
        }
        data.instructor = instructor;
        data.showStats = getRequestParamValue(Const.ParamsNames.FEEDBACK_RESULTS_SHOWSTATS);
        data.groupByTeam = getRequestParamValue(Const.ParamsNames.FEEDBACK_RESULTS_GROUPBYTEAM);
        data.sortType = getRequestParamValue(Const.ParamsNames.FEEDBACK_RESULTS_SORTTYPE);
        if (data.sortType == null) {
            // default: sort by recipients, stats shown.
            data.showStats = new String("on");
            data.sortType = new String("question");
        }
        data.sections = logic.getSectionNamesForCourse(courseId);

        if (data.selectedSection.equals(ALL_SECTION_OPTION)) {
            data.bundle = logic.getFeedbackSessionResultsForInstructorWithinRange(
                    feedbackSessionName, courseId, data.instructor.email, 5);
        } else if (data.sortType.equals("question")) {
            data.bundle = logic
                    .getFeedbackSessionResultsForInstructorInSection(
                            feedbackSessionName, courseId,
                            data.instructor.email, data.selectedSection);
        } else if (data.sortType.equals("giver-question-recipient")
                || data.sortType.equals("giver-recipient-question")) {
            data.bundle = logic
                    .getFeedbackSessionResultsForInstructorFromSection(
                            feedbackSessionName, courseId,
                            data.instructor.email, data.selectedSection);
        } else if (data.sortType.equals("recipient-question-giver")
                || data.sortType.equals("recipient-giver-question")) {
            data.bundle = logic
                    .getFeedbackSessionResultsForInstructorToSection(
                            feedbackSessionName, courseId,
                            data.instructor.email, data.selectedSection);
        }

        if (data.bundle == null) {
            throw new EntityDoesNotExistException(
                    "Feedback session " + feedbackSessionName + " does not exist in " + courseId + ".");
        }
        Iterator<FeedbackResponseAttributes> iterResponse = data.bundle.responses.iterator();
        while (iterResponse.hasNext()) {
            FeedbackResponseAttributes response = iterResponse.next();
            if ((!data.instructor.isAllowedForPrivilege(response.giverSection,
                    response.feedbackSessionName, Const.ParamsNames.INSTRUCTOR_PERMISSION_VIEW_SESSION_IN_SECTIONS))
                    || !(data.instructor.isAllowedForPrivilege(response.recipientSection,
                            response.feedbackSessionName, Const.ParamsNames.INSTRUCTOR_PERMISSION_VIEW_SESSION_IN_SECTIONS))) {
                data.bundle.responseComments.remove(response.getId());
                iterResponse.remove();
            }
        }
        
        switch (data.sortType) {
        case "question":
            return createShowPageResult(
                    Const.ViewURIs.INSTRUCTOR_FEEDBACK_RESULTS_BY_QUESTION,
                    data);
        case "recipient-giver-question":
            return createShowPageResult(
                    Const.ViewURIs.INSTRUCTOR_FEEDBACK_RESULTS_BY_RECIPIENT_GIVER_QUESTION,
                    data);
        case "giver-recipient-question":
            return createShowPageResult(
                    Const.ViewURIs.INSTRUCTOR_FEEDBACK_RESULTS_BY_GIVER_RECIPIENT_QUESTION,
                    data);
        case "recipient-question-giver":
            return createShowPageResult(
                    Const.ViewURIs.INSTRUCTOR_FEEDBACK_RESULTS_BY_RECIPIENT_QUESTION_GIVER,
                    data);
        case "giver-question-recipient":
            return createShowPageResult(
                    Const.ViewURIs.INSTRUCTOR_FEEDBACK_RESULTS_BY_GIVER_QUESTION_RECIPIENT,
                    data);
        default:
            data.sortType = "recipient-giver-question";
            return createShowPageResult(
                    Const.ViewURIs.INSTRUCTOR_FEEDBACK_RESULTS_BY_RECIPIENT_GIVER_QUESTION,
                    data);
        }
    }
}
