package com.temirlan.forms_to_moodle;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class QuizScraper {

    public static List<MoodleQuestion> scrapeQuestions(WebDriver driver) {
        List<MoodleQuestion> questions = new ArrayList<>();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        List<WebElement> questionDivs = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(
                By.cssSelector("div[data-automation-id='questionContent']")
        ));

        for (WebElement qDiv : questionDivs) {
            MoodleQuestion mq = new MoodleQuestion();

            WebElement questionTitleDiv = qDiv.findElement(
                    By.cssSelector("div[data-automation-id='questionTitle']")
            );

            mq.setQuestionText(getQuestionText(questionTitleDiv));
            mq.setQuestionType("Single Choice");

            //    "div[data-automation-id='questionChoiceOptionContainer']"
            List<WebElement> optionDivs = qDiv.findElements(
                    By.cssSelector("div[data-automation-id='questionChoiceOptionContainer']")
            );

            List<String> answers = new ArrayList<>();
            List<Integer> correctAnswerIndices = new ArrayList<>();

            //  For each option, check its text and aria-checked
            for (int i = 0; i < optionDivs.size(); i++) {
                WebElement option = optionDivs.get(i);

                // (a) Option text is again in a "span.text-format-content"
                //     in that option container
                WebElement optionTextSpan = option.findElement(By.cssSelector("span.text-format-content"));
                String optionText = optionTextSpan.getText().trim();
                answers.add(optionText);

                // (b) Check if it's correct. If "aria-checked='true'", it's correct
                int path = option.findElements(By.tagName("path")).size();
                if (path == 2) {
                    correctAnswerIndices.add(i);
                }
            }

            // Store answers and their correct indices
            mq.setAnswers(answers);
            mq.setCorrectAnswerIndices(correctAnswerIndices);

            questions.add(mq);
        }

        return questions;
    }

    private static String getQuestionType(WebElement questionTitleDiv) {
        WebElement questionInfoSpan = questionTitleDiv.findElement(
                By.xpath(".//span[starts-with(@id,'QuestionInfo_')]")
        );
        return questionInfoSpan.getText().trim();
    }

    private static String getQuestionText(WebElement questionTitleDiv) {
        WebElement firstSpan = questionTitleDiv.findElement(By.xpath(".//span[1]"));
        List<WebElement> subSpans = firstSpan.findElements(By.tagName("span"));
        String questionNumber = subSpans.get(0).getText().trim();
        String questionTextPart = subSpans.get(1).getText().trim();
        String combinedQuestionText = questionNumber + " " + questionTextPart;
        return combinedQuestionText;
    }

    private static void extractQuestionTitle(WebElement qDiv, MoodleQuestion mq) {


        // 2) Extract question text.
        //    Usually there's a <span ... class="text-format-content"> for the question prompt.
        //    But sometimes you might have multiple spans. Let's collect them all and join.
        List<WebElement> questionTextSpans = qDiv.findElements(
                By.cssSelector("span.text-format-content")
        );

        // We assume the very first span is the question prompt or at least part of it
        // (If some are sub-headers, you may need more robust logic.)
        StringBuilder sb = new StringBuilder();
        for (WebElement span : questionTextSpans) {
            String spanText = span.getText().trim();
            // Exclude empty or purely symbolic spans if needed
            if (!spanText.isEmpty()) {
                sb.append(spanText).append(" ");
            }
        }
        mq.setQuestionText(sb.toString().trim());
    }
}
