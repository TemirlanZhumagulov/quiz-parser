package com.temirlan.moodle_to_forms;

import org.apache.commons.io.FileUtils;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MoodleXmlToFormsDocxConverter {

    private static final String TODO_ADD_NEW_LINE = "(TODO: NEW LINE)";
    private static final String TODO_ADD_NEW_IMAGE = "(TODO: REPLACE THIS WITH THE IMAGE: %s)";
    private static final String TODO_ADD_CORRECT_ANSWER = "(TODO: ADD THIS TO THE CORRECT ANSWERS)";

    private static String IMAGES_DIRECTORY;

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("How to use it: java MoodleXmlToFormsDocxConverter <inputXML> <outputDocx> <imageDirectory>");
            return;
        }

        File inputFile = new File(args[0]);
        File outputFile = new File(args[1]);
        IMAGES_DIRECTORY = args[2];

        try {
            parseMoodleXmlFileToMicrosoftFormsFormat(inputFile, outputFile);

            System.out.println("Successfully generated Word document: " + outputFile.getAbsolutePath());
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void parseMoodleXmlFileToMicrosoftFormsFormat(File inputFile, File outputFile)
            throws ParserConfigurationException, SAXException, IOException {

        IMAGES_DIRECTORY = addBackslashAtEnd(IMAGES_DIRECTORY);

        XWPFDocument wordDocument = new XWPFDocument();

        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputFile);
        doc.getDocumentElement().normalize();

        NodeList questionList = doc.getElementsByTagName("question");

        for (int i = 0, questionNumber = 1; i < questionList.getLength(); i++) {
            Node questionNode = questionList.item(i);

            if (questionNode.getNodeType() == Node.ELEMENT_NODE) {
                Element questionElement = (Element) questionNode;

                String questionType = questionElement.getAttribute("type");

                if ("category".equals(questionType)) {
                    continue;
                }

                switch (questionType) {
                    case "multichoice", "numerical", "shortanswer" ->
                            processMultipleChoiceQuestion(wordDocument, questionElement, questionNumber);
                    case "truefalse" -> processTrueFalseQuestion(wordDocument, questionElement, questionNumber);
                    case "cloze" -> processClozeQuestion(wordDocument, questionElement, questionNumber);
                    case "gapselect" -> processGapSelectQuestion(wordDocument, questionElement, questionNumber, false);
                    case "ddwtos" -> processGapSelectQuestion(wordDocument, questionElement, questionNumber, true);
                    default -> throw new RuntimeException("Unknown question type: " + questionType);
                }

                questionNumber++;
                createParagraph(wordDocument, ""); // blank line
            }
        }

        // Write the document to the output file
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            wordDocument.write(fos);
        }
    }

    private static void processMultipleChoiceQuestion(XWPFDocument doc, Element questionElement, int questionNumber) throws IOException {
        String questionText = extractQuestionText(questionElement)
                .replaceAll("\n", TODO_ADD_NEW_LINE);
        createParagraph(doc, questionNumber + ". " + questionText);

        NodeList answerList = questionElement.getElementsByTagName("answer");
        char option = 'A';
        String correctAnswer = "";

        for (int j = 0; j < answerList.getLength(); j++) {
            Node answerNode = answerList.item(j);

            if (answerNode.getNodeType() == Node.ELEMENT_NODE) {
                Element answerElement = (Element) answerNode;

                String answerText = extractTextWithoutHtml(answerElement);

                if (!"0".equals(answerElement.getAttribute("fraction"))) {
                    if (!correctAnswer.isEmpty()) {
                        answerText += TODO_ADD_CORRECT_ANSWER;
                    } else {
                        correctAnswer = String.valueOf(option);
                    }
                }

                // Write option to doc
                createParagraph(doc, option + ") " + answerText);
                option++;
            }
        }

        if (correctAnswer.isEmpty()) {
            throw new RuntimeException("There is no correct answer for a multiple choice question.");
        }

        createParagraph(doc, "Answer: " + correctAnswer + ")");
    }

    private static void processTrueFalseQuestion(XWPFDocument doc, Element questionElement, int questionNumber) throws IOException {
        String questionText = extractQuestionText(questionElement)
                .replaceAll("\n", TODO_ADD_NEW_LINE);
        createParagraph(doc, questionNumber + ". " + questionText);

        NodeList answerList = questionElement.getElementsByTagName("answer");
        String correctAnswer = "";

        for (int j = 0; j < answerList.getLength(); j++) {
            Node answerNode = answerList.item(j);

            if (answerNode.getNodeType() == Node.ELEMENT_NODE) {
                Element answerElement = (Element) answerNode;

                String answerText = extractTextWithoutHtml(answerElement);

                if (!"0".equals(answerElement.getAttribute("fraction"))) {
                    if (!correctAnswer.isEmpty()) {
                        throw new RuntimeException("There is more than one correct answer for a true/false question.");
                    }
                    correctAnswer = answerText;
                }
            }
        }

        if (correctAnswer.isEmpty()) {
            throw new RuntimeException("There is no correct answer for a true/false question.");
        }

        createParagraph(doc, "A) true");
        createParagraph(doc, "B) false");

        String answerLetter = "true".equalsIgnoreCase(correctAnswer) ? "A" : "B";
        createParagraph(doc, "Answer: " + answerLetter);
    }

    private static final Pattern CLOZE_BLOCK_PATTERN = Pattern.compile("\\{(.*?)}");

    private static void processClozeQuestion(XWPFDocument doc, Element questionElement, int questionNumber) throws IOException {
        String questionText = extractQuestionText(questionElement);

        List<String> blocks = extractClozeBlocks(questionText.replaceAll("\n", ""));

        createParagraph(doc, questionNumber + ". " + questionText.split("\\{")[0].trim().replaceAll("\n", TODO_ADD_NEW_LINE));

        for (String block : blocks) {
            String[] parts = block.split(":", 3);
            String questionId = "";
            String questionType = "";
            String answerSection = "";

            if (parts.length == 3) {
                questionId = parts[0];
                questionType = parts[1];
                answerSection = parts[2];
            } else {
                throw new RuntimeException("There is more than one cloze block for a question.");
            }

            String[] rawAnswers = answerSection.split("~");
            char option = 'A';
            String correctAnswer = "";

            for (int i = 0; i < rawAnswers.length; i++) {
                String ans = rawAnswers[i];
                if (ans.isEmpty()) continue;
                if (ans.startsWith("=")) {
                    if (!correctAnswer.isEmpty()) {
                        ans += TODO_ADD_CORRECT_ANSWER;
                    } else {
                        correctAnswer = String.valueOf(option);
                    }
                } else if (ans.startsWith("%")) {
                    String[] split = ans.split("%");
                    if (split.length != 3) {
                        throw new RuntimeException("Embedded question of MULTICHOICE has changed.");
                    }
                    ans = split[2];
                    if (!correctAnswer.isEmpty()) {
                        ans += TODO_ADD_CORRECT_ANSWER;
                    } else
                        correctAnswer = String.valueOf(option);
                }
                // Remove any leading = or ~
                String cleaned = ans.replaceFirst("^[=]*", "").trim();

                createParagraph(doc, option + ") " + cleaned);
                option++;
            }

            if (correctAnswer.isEmpty()) {
                throw new RuntimeException("There is no correct answer for a cloze question.");
            }

            createParagraph(doc, "Answer: " + correctAnswer);
        }

    }

    private static List<String> extractClozeBlocks(String clozeText) {
        List<String> results = new ArrayList<>();
        Matcher matcher = CLOZE_BLOCK_PATTERN.matcher(clozeText);
        while (matcher.find()) {
            String inside = matcher.group(1).trim(); // The content between { and }
            results.add(inside);
        }
        return results;
    }

    private static void processGapSelectQuestion(XWPFDocument doc, Element questionElement, int questionNumber, boolean dragAndDrop) throws IOException {
        String questionText = extractQuestionText(questionElement)
                .replaceAll("\n", TODO_ADD_NEW_LINE);
        createParagraph(doc, questionNumber + ". " + questionText.replaceAll("\\[\\[(\\d+)]]", "______"));

        List<String> placeholders = findPlaceholders(questionText); // e.g. returns ["[[1]]", "[[3]]", ...]

        Map<Integer, List<String>> groupToChoices = parseSelectOptions(questionElement, dragAndDrop);

        if (groupToChoices.size() > 1) {
            throw new RuntimeException("More than 1 group is found, cannot handle this case");
        }

        char option = 'A';
        String correctAnswer = "";
        Iterator<String> iterator = placeholders.iterator();
        int corNum = iterator.hasNext() ? parsePlaceholderNumber(iterator.next()) : -1;
        int groupId = 1;

        List<String> choices = groupToChoices.getOrDefault(groupId, Collections.emptyList());

        for (int i = 0; i < choices.size(); i++) {
            String answerText = choices.get(i);
            if (i == corNum - 1) {
                if (!correctAnswer.isEmpty()) {
                    answerText += TODO_ADD_CORRECT_ANSWER;
                } else {
                    correctAnswer = String.valueOf(option);
                }
                corNum = iterator.hasNext() ? parsePlaceholderNumber(iterator.next()) : -1;
            }
            createParagraph(doc, option + ") " + answerText);
            option++;
        }

        if (correctAnswer.isEmpty()) {
            throw new RuntimeException("There is no correct answer for a " + (dragAndDrop ? "drag and drop" : "gap select") + " question.");
        }

        createParagraph(doc, "Answer: " + correctAnswer);
    }

    private static Map<Integer, List<String>> parseSelectOptions(Element questionEl, boolean dragAndDrop) {
        Map<Integer, List<String>> map = new HashMap<>();

        NodeList selectNodes = dragAndDrop ?
                questionEl.getElementsByTagName("dragbox") :
                questionEl.getElementsByTagName("selectoption");

        for (int i = 0; i < selectNodes.getLength(); i++) {
            Element selEl = (Element) selectNodes.item(i);
            String textVal = getElementText(selEl, "text").trim();
            int groupId = Integer.parseInt(getElementText(selEl, "group").trim());

            map.computeIfAbsent(groupId, g -> new ArrayList<>()).add(textVal);
        }
        return map;
    }

    private static String getElementText(Element parent, String tagName) {
        NodeList nl = parent.getElementsByTagName(tagName);
        if (nl.getLength() > 0) {
            return nl.item(0).getTextContent();
        }
        return "";
    }

    private static List<String> findPlaceholders(String text) {
        List<String> placeholders = new ArrayList<>();

        Matcher m = Pattern.compile("\\[\\[(\\d+)]]").matcher(text);

        while (m.find()) {
            String match = m.group(0);  // e.g. "[[3]]"
            placeholders.add(match);
        }

        return placeholders;
    }

    private static Integer parsePlaceholderNumber(String placeholder) {
        // placeholder is e.g. "[[3]]"
        // strip brackets:
        String justNum = placeholder.replaceAll("[^\\d]+", "");
        try {
            return Integer.parseInt(justNum);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // region help methods
    private static String addBackslashAtEnd(String imagesDirectory) {
        if (imagesDirectory.charAt(imagesDirectory.length() - 1) != '\\') {
            return imagesDirectory + "\\";
        }
        return imagesDirectory;
    }

    private static String extractQuestionText(Element questionElement) throws IOException {
        Element qTextEl = (Element) questionElement.getElementsByTagName("questiontext").item(0);
        String rawHtml = qTextEl.getElementsByTagName("text").item(0).getTextContent();

        Map<String, String> fileMap = extractAndSaveImages(qTextEl);

        rawHtml = replaceImgTagsWithPlaceholders(rawHtml, fileMap);

        String cleaned = rawHtml
                .replaceAll("(?s)<p>(.*?)</p>", "$1\n")
                .replaceAll("(?s)<br>", "\n")
                .replaceAll("<[^>]*>", "")
                .replaceAll(" |&nbsp;", " ")
                .replaceAll("[ \t]+", " ")
                .replaceAll("\\s+\n", "\n");

        return cleaned.trim();
    }

    private static Map<String, String> extractAndSaveImages(Element questionTextEl) throws IOException {
        Map<String, String> resultMap = new HashMap<>();

        NodeList fileNodes = questionTextEl.getElementsByTagName("file");
        for (int i = 0; i < fileNodes.getLength(); i++) {
            Node fNode = fileNodes.item(i);
            if (fNode.getNodeType() == Node.ELEMENT_NODE) {
                Element fEl = (Element) fNode;
                String filename = fEl.getAttribute("name");
                if (filename.toLowerCase().matches(".*\\.(png|jpg|jpeg|gif)$")) {
                    String fullPath = IMAGES_DIRECTORY + filename;
                    resultMap.put(filename, fullPath);

                    byte[] data = Base64.getDecoder().decode(fEl.getTextContent().trim());
                    FileUtils.writeByteArrayToFile(new File(fullPath), data);
                }
            }
        }
        return resultMap;
    }

    private static String replaceImgTagsWithPlaceholders(String html, Map<String, String> fileMap) {
        Pattern imgPattern = Pattern.compile("(?i)<img[^>]*src=\"@@PLUGINFILE@@/(.*?)\"[^>]*>");
        Matcher matcher = imgPattern.matcher(html);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String filename = matcher.group(1);
            String path = fileMap.getOrDefault(filename, filename);
            String replacement = String.format(TODO_ADD_NEW_IMAGE, path);
            // Escape backslashes, etc. if needed
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private static String extractTextWithoutHtml(Element qTextEl) {
        return qTextEl.getElementsByTagName("text")
                .item(0)
                .getTextContent()
                .replaceAll("(?s)<p>(.*?)</p>", "$1\n")
                .replaceAll("(?s)<br>", "\n")
                .replaceAll("<[^>]*>", "")
                .replaceAll(" |&nbsp;", " ")
                .trim();
    }

    private static void createParagraph(XWPFDocument doc, String text) {
        XWPFParagraph paragraph = doc.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(text);
    }
// endregion help methods

}
