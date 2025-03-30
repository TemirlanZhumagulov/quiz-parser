package com.temirlan.forms_to_moodle;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class WordExporter {
    public static void writeQuestionsToWord(List<MoodleQuestion> questionList, String outputPath) {
        XWPFDocument doc = new XWPFDocument();

        File outFile = new File(outputPath);
        if (outFile.getParentFile() != null && !outFile.getParentFile().exists()) {
            outFile.getParentFile().mkdirs();
        }

        for (MoodleQuestion mq : questionList) {

            XWPFParagraph questionParagraph = doc.createParagraph();
            XWPFRun questionRun = questionParagraph.createRun();


            questionRun.setText(mq.getQuestionText());

            List<String> answers = mq.getAnswers();
            for (int ansIndex = 0; ansIndex < answers.size(); ansIndex++) {
                // Convert 0->A, 1->B, 2->C, ...
                char letter = (char) ('A' + ansIndex);

                // e.g. "A) foo"
                String answerLine = letter + ") " + answers.get(ansIndex);

                // New paragraph (or you can just add a line break)
                XWPFParagraph answerParagraph = doc.createParagraph();
                XWPFRun answerRun = answerParagraph.createRun();
                answerRun.setText(answerLine);
            }

            List<Integer> correctIndices = mq.getCorrectAnswerIndices();
            StringBuilder correctLine = new StringBuilder("Answer: ");
            for (int c = 0; c < correctIndices.size(); c++) {
                // e.g. 1 -> 'B'
                char letter = (char) ('A' + correctIndices.get(c));
                correctLine.append(letter);
                if (c < correctIndices.size() - 1) {
                    correctLine.append(", ");
                }
            }

            XWPFParagraph answerKeyPara = doc.createParagraph();
            XWPFRun answerKeyRun = answerKeyPara.createRun();
            answerKeyRun.setText(correctLine.toString());

            // Add an extra blank line between questions
//            XWPFParagraph spacer = doc.createParagraph();
//            spacer.createRun().addCarriageReturn();
        }


        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            doc.write(fos);
            System.out.println("Questions exported to " + outputPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
