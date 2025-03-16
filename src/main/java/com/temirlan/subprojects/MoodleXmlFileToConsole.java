package com.temirlan.subprojects;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

public class MoodleXmlFileToConsole {
    public static void main(String[] args) {
        try {

            parseMoodleXmlFileToMicrosoftFormsFormat(new File("C:\\Users\\temir\\Downloads\\questions-oopExam-20250227-2135.xml"));

        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void parseMoodleXmlFileToMicrosoftFormsFormat(File inputFile) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(inputFile);
        doc.getDocumentElement().normalize();

        NodeList questionList = doc.getElementsByTagName("question");
        int questionNumber = 1;

        for (int i = 0; i < questionList.getLength(); i++) {
            Node questionNode = questionList.item(i);

            if (questionNode.getNodeType() == Node.ELEMENT_NODE) {
                Element questionElement = (Element) questionNode;
                String questionType = questionElement.getAttribute("type");

                // Process multiple-choice questions
                if ("multichoice".equals(questionType)) {
                    processMultipleChoiceQuestion(questionElement, questionNumber);
                    questionNumber++;
                }
                // Process true/false questions
                else if ("truefalse".equals(questionType)) {
                    processTrueFalseQuestion(questionElement, questionNumber);
                    questionNumber++;
                }
            }
        }
    }

    private static void processMultipleChoiceQuestion(Element questionElement, int questionNumber) {
        String questionText = extractAndStripHtmlText(questionElement, "questiontext");

        questionText = removePrefix(questionText);

        System.out.println(questionNumber + ". " + questionText);

        NodeList answerList = questionElement.getElementsByTagName("answer");
        char option = 'A';
        String correctAnswer = "";

        for (int j = 0; j < answerList.getLength(); j++) {
            Node answerNode = answerList.item(j);

            if (answerNode.getNodeType() == Node.ELEMENT_NODE) {
                Element answerElement = (Element) answerNode;

                String answerText = extractAndStripHtmlText(questionElement, "text");

                boolean isMultipleCorrectAnswers = false;
                if (!"0".equals(answerElement.getAttribute("fraction"))) {
                    if (!correctAnswer.isEmpty()) {
                        isMultipleCorrectAnswers = true;
                    }
                    correctAnswer = String.valueOf(option);
                }

                if (isMultipleCorrectAnswers) {
                    continue;
                }

                System.out.println(option + ") " + answerText);

                option++;
            }
        }

        System.out.println("Answer: " + correctAnswer);
        System.out.println();
    }

    private static String extractAndStripHtmlText(Element questionElement, String tagName) {
        return questionElement.getElementsByTagName(tagName)
                .item(0).getTextContent().replaceAll("<[^>]*>", "").trim();
    }

    private static void processTrueFalseQuestion(Element questionElement, int questionNumber) {
        String questionText = extractAndStripHtmlText(questionElement, "questiontext");

        questionText = removePrefix(questionText);

        System.out.println(questionNumber + ". " + questionText);

        // Get all answer nodes
        NodeList answerList = questionElement.getElementsByTagName("answer");
        String correctAnswer = "";

        for (int j = 0; j < answerList.getLength(); j++) {
            Node answerNode = answerList.item(j);

            if (answerNode.getNodeType() == Node.ELEMENT_NODE) {
                Element answerElement = (Element) answerNode;

                String answerText = answerElement.getElementsByTagName("text")
                        .item(0).getTextContent().trim();

                boolean isMultipleCorrectAnswers = false;
                if (!"0".equals(answerElement.getAttribute("fraction"))) {
                    if (!correctAnswer.isEmpty()) {
                        throw new RuntimeException("There are more than one correct answer");
                    }
                    correctAnswer = answerText;
                }
            }
        }

        System.out.println("A) true");
        System.out.println("B) false");

        String answerLetter = "true".equals(correctAnswer) ? "a" : "b";
        System.out.println("Answer: " + answerLetter);
        System.out.println();
    }

    private static String removePrefix(String text) {
        int dotIndex = text.indexOf('.');
        if (dotIndex != -1) {
            return text.substring(dotIndex + 1).trim();
        }
        return text;
    }


}