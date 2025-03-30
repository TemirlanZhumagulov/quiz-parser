package com.temirlan.forms_to_moodle;

import java.util.List;

public class MoodleQuestion {

    private String questionText;    // e.g. "CHA9-E. WHICH OF THE FOLLOWING TYPES..."
    private String questionType;    // e.g. "Multiple choice"
    private List<String> answers;   // e.g. [ "Answer A text", "Answer B text", ... ]
    private List<Integer> correctAnswerIndices; // which indices in the 'answers' are correct?


    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public List<String> getAnswers() {
        return answers;
    }

    public void setAnswers(List<String> answers) {
        this.answers = answers;
    }

    public List<Integer> getCorrectAnswerIndices() {
        return correctAnswerIndices;
    }

    public void setCorrectAnswerIndices(List<Integer> correctAnswerIndices) {
        this.correctAnswerIndices = correctAnswerIndices;
    }

}
