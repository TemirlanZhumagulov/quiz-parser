package com.temirlan.subprojects;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MoodleXmlFileToDocxFile {

    public static void main(String[] args) {
        try {
            File inputFile = new File("C:\\Users\\temir\\Downloads\\questions-oopExam-20250227-2135.xml");
            File outputFile = new File("C:\\Users\\temir\\Downloads\\generatedQuestions.docx");

            parseMoodleXmlFileToMicrosoftFormsFormat(inputFile, outputFile);
            System.out.println("Successfully generated Word document: " + outputFile.getAbsolutePath());
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void parseMoodleXmlFileToMicrosoftFormsFormat(File inputFile, File outputFile)
            throws ParserConfigurationException, SAXException, IOException {

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(inputFile);
        doc.getDocumentElement().normalize();

        XWPFDocument wordDocument = new XWPFDocument();

        NodeList questionList = doc.getElementsByTagName("question");
        int questionNumber = 1;

        for (int i = 0; i < questionList.getLength(); i++) {
            Node questionNode = questionList.item(i);

            if (questionNode.getNodeType() == Node.ELEMENT_NODE) {
                Element questionElement = (Element) questionNode;
                String questionType = questionElement.getAttribute("type");

                // Process multiple-choice questions
                if ("multichoice".equals(questionType)) {
                    processMultipleChoiceQuestion(wordDocument, questionElement, questionNumber);
                    questionNumber++;
                }
                // Process true/false questions
                else if ("truefalse".equals(questionType)) {
                    processTrueFalseQuestion(wordDocument, questionElement, questionNumber);
                    questionNumber++;
                }
            }
        }

        // Write the document to the output file
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            wordDocument.write(fos);
        }
    }

    private static void processMultipleChoiceQuestion(XWPFDocument doc, Element questionElement, int questionNumber) {
        String questionText = extractAndStripHtmlText(questionElement);
        questionText = removePrefix(questionText).trim();
        String[] split = questionText.split("\n");
        // Write question text
        for (int i = 0; i < split.length; i++) {
            if (i == 0) {
                createParagraph(doc, questionNumber + ". " + split[i]);
            } else {
                createParagraph(doc, split[i]);
            }
        }

        NodeList answerList = questionElement.getElementsByTagName("answer");
        char option = 'A';
        String correctAnswer = "";

        for (int j = 0; j < answerList.getLength(); j++) {
            Node answerNode = answerList.item(j);

            if (answerNode.getNodeType() == Node.ELEMENT_NODE) {
                Element answerElement = (Element) answerNode;

                String answerText = answerElement
                        .getElementsByTagName("text")
                        .item(0)
                        .getTextContent()
                        .replaceAll("<[^>]*>", "")
                        .trim();

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

                // Write option to doc
                createParagraph(doc, option + ") " + answerText);
                option++;
            }
        }

        createParagraph(doc, "Answer: " + correctAnswer);
        createParagraph(doc, ""); // blank line
    }

    private static void processTrueFalseQuestion(XWPFDocument doc, Element questionElement, int questionNumber) {
        String questionText = extractAndStripHtmlText(questionElement);
        questionText = removePrefix(questionText).trim();
        String[] split = questionText.split("\n");
        // Write question text
        for (int i = 0; i < split.length; i++) {
            if (i == 0) {
                createParagraph(doc, questionNumber + ". " + split[i]);
            } else {
                createParagraph(doc, split[i]);
            }
        }

        NodeList answerList = questionElement.getElementsByTagName("answer");
        String correctAnswer = "";

        for (int j = 0; j < answerList.getLength(); j++) {
            Node answerNode = answerList.item(j);

            if (answerNode.getNodeType() == Node.ELEMENT_NODE) {
                Element answerElement = (Element) answerNode;

                String answerText = answerElement
                        .getElementsByTagName("text")
                        .item(0)
                        .getTextContent()
                        .trim();

                if (!"0".equals(answerElement.getAttribute("fraction"))) {
                    if (!correctAnswer.isEmpty()) {
                        throw new RuntimeException("There is more than one correct answer for a true/false question.");
                    }
                    correctAnswer = answerText;
                }
            }
        }

        createParagraph(doc, "A) true");
        createParagraph(doc, "B) false");

        String answerLetter = "true".equalsIgnoreCase(correctAnswer) ? "A" : "B";
        createParagraph(doc, "Answer: " + answerLetter);
        createParagraph(doc, ""); // blank line
    }

    private static String extractAndStripHtmlText(Element questionElement) {
        return questionElement
                .getElementsByTagName("questiontext")
                .item(0)
                .getTextContent()
                .replaceAll("<[^>]*>", "")
                .replaceAll(" ", " ");
    }

    private static String removePrefix(String text) {
        int dotIndex = text.indexOf('.');
        if (dotIndex != -1) {
            return text.substring(dotIndex + 1).trim();
        }
        return text;
    }

    private static void createParagraph(XWPFDocument doc, String text) {
        XWPFParagraph paragraph = doc.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(text);
    }
}
