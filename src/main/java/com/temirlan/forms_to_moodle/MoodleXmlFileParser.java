package com.temirlan.forms_to_moodle;

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
import java.util.ArrayList;
import java.util.List;

public class MoodleXmlFileParser {

//    public static void main(String[] args) {
//        try {
//
//            parseMoodleXml(new File("C:\\Users\\temir\\Downloads\\questions-oopExam-20250227-2135.xml"));
//
//        } catch (ParserConfigurationException | SAXException | IOException e) {
//            throw new RuntimeException(e);
//        }
//
//    }

    public static List<MoodleQuestion> parseMoodleXml(File xmlFile)
            throws ParserConfigurationException, SAXException, IOException {

        List<MoodleQuestion> parsedQuestions = new ArrayList<>();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(xmlFile);
        doc.getDocumentElement().normalize();

        NodeList questionNodes = doc.getElementsByTagName("question");
        for (int i = 0; i < questionNodes.getLength(); i++) {
            Node questionNode = questionNodes.item(i);
            if (questionNode.getNodeType() == Node.ELEMENT_NODE) {
                Element questionElement = (Element) questionNode;
                // e.g. multichoice, truefalse, shortanswer, etc.
                String questionType = questionElement.getAttribute("type");

                // We'll skip question type="category" or other specialized ones
                if ("category".equals(questionType)) {
                    continue;
                }

                String questionText = getElementText(questionElement, "questiontext");

                MoodleQuestion mq = new MoodleQuestion();
                mq.setQuestionText(stripHtml(questionText));
                mq.setQuestionType(questionType);

                //    fraction="0" means incorrect, fraction="100" (or 50, etc.) means correct
                NodeList answerNodes = questionElement.getElementsByTagName("answer");
                List<String> answers = new ArrayList<>();
                List<Integer> correctAnswerIndices = new ArrayList<>();

                for (int j = 0; j < answerNodes.getLength(); j++) {
                    Element answerEl = (Element) answerNodes.item(j);

                    // fraction attribute, e.g. "100" or "0"
                    String fractionStr = answerEl.getAttribute("fraction");
                    double fractionVal = Double.parseDouble(fractionStr);

                    // <text> for the answer text
                    String answerText = getTextContent(answerEl, "text");
                    answerText = stripHtml(answerText);

                    answers.add(answerText);

                    // If fraction is not 0, we consider it correct (or partially correct)
                    if (fractionVal != 0.0) {
                        correctAnswerIndices.add(j);
                    }
                }

                mq.setAnswers(answers);
                mq.setCorrectAnswerIndices(correctAnswerIndices);

                // Add to our list
                parsedQuestions.add(mq);
            }
        }
        return parsedQuestions;
    }

    // Helper: gets the text content from <tagName><text>...</text>
    private static String getElementText(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            Element e = (Element) nodeList.item(0);
            return getTextContent(e, "text");
        }
        return "";
    }

    private static String getTextContent(Element parent, String childTagName) {
        NodeList nl = parent.getElementsByTagName(childTagName);
        if (nl.getLength() > 0) {
            return nl.item(0).getTextContent();
        }
        return "";
    }

    // Helper: strip basic HTML tags
    private static String stripHtml(String text) {
        // naive approach
        return text.replaceAll("<[^>]*>", "").trim();
    }

}
