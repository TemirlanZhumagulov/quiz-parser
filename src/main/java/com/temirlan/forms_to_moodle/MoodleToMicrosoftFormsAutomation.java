package com.temirlan.forms_to_moodle;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class MoodleToMicrosoftFormsAutomation {

    public static void main(String[] args) {
        WebDriverManager.chromedriver().setup();

        System.setProperty("webdriver.chrome.driver", "C:/Users/temir/Downloads/chromedriver-win64/chromedriver-win64/chromedriver.exe");

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);

        WebDriver driver = new ChromeDriver(options);

        String moodleUsername = "";
        String moodlePassword = "";

        String msUsername = "";
        String msPassword = "";

        try {
            driver.get("https://www.office.com");

            login(driver, msUsername, msPassword);

            driver.get("https://forms.office.com/");

            WebElement gotItBtn = findElementNoThrow(driver, By.xpath("//button[@aria-label='Close']"), 5, 1000);
            // Or maybe "button[aria-label='Close']"
            if (gotItBtn != null) {
                gotItBtn.click();
            }

            String windowHandle1 = driver.getWindowHandle();

            WebElement elementWithRetries = findElementWithRetries(driver,
                    By.id("form-item-XhsIV2rmk0mOrxWwswkpP8w3Kcr8csBHkvvLp68RM-JUNEdDNFoxSUwyTEREWVBaOThCTks5MUU1Qi4u"),
                    10, 1000);

            elementWithRetries.click();

            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(d -> d.getWindowHandles().size() == 2);

            for (String windowHandle : driver.getWindowHandles()) {
                if (!windowHandle.equals(windowHandle1)) {
                    driver.switchTo().window(windowHandle);
                    break;
                }
            }

//            findElementWithRetries(driver, By.cssSelector("button[aria-label='Preview']"), 10, 1000)
//                    .click();

            List<MoodleQuestion> questions = QuizScraper.scrapeQuestions(driver);

            String timestamp = "2025-03-17T03:27:19.582506";
            String safeTimestamp = timestamp.replace(':', '-');
// becomes "2025-03-17T03-27-19.582506"

            String filename = "forms-questions-" + safeTimestamp + ".docx";
            String outputPath = "C:\\Users\\temir\\Downloads\\" + filename;

            WordExporter.writeQuestionsToWord(questions, outputPath);
//
//            List<MoodleQuestion> questions = MoodleXmlParser.
//                    parseMoodleXml(new File("C:\\Users\\temir\\Downloads\\questions-oopExam-20250227-2135.xml"));
//
//            for (MoodleQuestion question : questions) {
//                addQuestion(driver, question);
//            }


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Wait a bit to see result, then close
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            }
            driver.quit();
        }
    }

    private static void login(WebDriver driver, String msUsername, String msPassword) throws InterruptedException {
        WebElement signInBtn = findElementWithRetries(driver, By.linkText("Sign in"), 10, 1000);
        signInBtn.click();

        WebElement emailField = findElementWithRetries(driver, By.id("i0116"), 100, 1000);
        emailField.sendKeys(msUsername);
        WebElement nextBtn1 = findElementWithRetries(driver, By.id("idSIButton9"), 20, 1000);
        nextBtn1.click();

        WebElement pwdField = findElementWithRetries(driver, By.id("i0118"), 50, 1000);
        pwdField.sendKeys(msPassword);
        Thread.sleep(3000); // todo it is needed to avoid
        WebElement nextBtn2 = findElementWithRetries(driver, By.id("idSIButton9"), 50, 1000);
        nextBtn2.click();

        try {
            WebElement noBtn = findElementWithRetries(driver, By.id("idBtn_Back"), 5, 1000);
            noBtn.click();
        } catch (NoSuchElementException | StaleElementReferenceException ex) {
            // Might not appear
        }
    }

    private static void createNewQuiz(WebDriver driver, String quizTitle) throws InterruptedException {
        WebElement newQuizBtn = findElementWithRetries(driver,
                By.xpath("//button[contains(text(),'New Quiz') or @aria-label='Create a new quiz']"),
                10, 1000);
        newQuizBtn.click();
        Thread.sleep(3000);

//        WebElement titleDiv = findElementWithRetries(driver,
//                By.xpath("//div[contains(text(),'Untitled quiz')]"),
//                10, 1000);
//        titleDiv.click();
//        Thread.sleep(1000);
//
//        titleDiv.sendKeys(Keys.CONTROL + "a");
//        titleDiv.sendKeys(Keys.DELETE);
//
//        titleDiv.sendKeys(quizTitle);
//
//        titleDiv.sendKeys(Keys.ENTER);
    }

    private static void addQuestion(WebDriver driver, MoodleQuestion q) throws InterruptedException {
//        WebElement addQuestionBtn = findElementWithRetries(driver,
//                By.cssSelector("button#add-question-button"),
//                10, 1000);
//        addQuestionBtn.click();
//        Thread.sleep(1500);
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");

        // Now the question type options appear: Choice, Text, etc.
        String type = q.getQuestionType().toLowerCase();
        if (type.contains("multi") || type.contains("truefalse")) {
            // click "Choice"
            WebElement choiceBtn = findElementWithRetries(driver,
                    By.xpath("//button[@aria-label='Choice']"),
                    10, 1000);
            choiceBtn.click();
        } else {
            // e.g. shortanswer => "Text"
            WebElement textBtn = findElementWithRetries(driver,
                    By.xpath("//button[@aria-label='Text']"),
                    10, 1000);
            textBtn.click();
        }
        Thread.sleep(2000);

        // Fill the question prompt
        WebElement questionTitleField = findElementWithRetries(
                driver,
                By.cssSelector("input[aria-label='Question']"), // or "textarea" if that's what's used
                10, 1000);
        questionTitleField.clear();
        questionTitleField.sendKeys(q.getQuestionText());
        Thread.sleep(500);

        // If multiple choice or T/F, fill out the answer options
        if (type.contains("multi") || type.contains("truefalse")) {
            List<String> answers = q.getAnswers();
            for (int i = 0; i < answers.size(); i++) {
                if (i < 2) {
                    WebElement optField = findElementWithRetries(
                            driver,
                            By.xpath("//input[@placeholder='Option " + (i + 1) + "']"),
                            10,
                            1000
                    );
                    optField.clear();
                    optField.sendKeys(answers.get(i));
                } else {
                    // If more than 2 options, click "Add option"
                    WebElement addOptionLink = findElementWithRetries(
                            driver,
                            By.xpath("//span[contains(text(),'Add option')]"),
                            10,
                            1000
                    );
                    addOptionLink.click();
                    Thread.sleep(500);

                    // Then find the newly created option field. Usually it's "Option 3", "Option 4", etc.
                    WebElement newOptField = findElementWithRetries(
                            driver,
                            By.xpath("//input[@placeholder='Option " + (i + 1) + "']"),
                            10,
                            1000
                    );
                    newOptField.sendKeys(answers.get(i));
                }

                if (q.getCorrectAnswerIndices().contains(i)) {
                }
            }
        }

        // Done with this question
        Thread.sleep(1000);
    }

    private static void fileImporting(WebDriver driver) throws InterruptedException {
        WebElement quickImportBtn = findElementWithRetries(driver,
                By.xpath("//button[@aria-label='Quick import']"),
                10, 1000);
        quickImportBtn.click();
        Thread.sleep(2000);

        WebElement uploadFromDevice = findElementWithRetries(driver,
                By.xpath("//div[contains(text(),'Upload from this device')]"),
                10, 1000);
        uploadFromDevice.click();
        Thread.sleep(1000);

        WebElement fileInput = findElementWithRetries(driver,
                By.xpath("//input[@type='file']"),
                10, 1000);
        fileInput.sendKeys("C:\\Users\\temir\\Downloads\\generatedQuestions.docx");

        Thread.sleep(5000); // or better: WebDriverWait for an upload-finished indicator
        WebElement importBtn = findElementWithRetries(driver,
                By.xpath("//button[contains(., 'Import')]"),
                10, 1000);
        importBtn.click();
    }

    private static void scrapingImplementation(WebDriver driver) throws InterruptedException {
        List<MoodleQuestion> questions = scrapeMoodleQuiz(driver);

        for (MoodleQuestion q : questions) {
            System.out.println("==================================");
            System.out.println("Text: " + q.getQuestionText());
            System.out.println("Type: " + q.getQuestionType());
            System.out.println("Answers:");
            for (int i = 0; i < q.getAnswers().size(); i++) {
                String ans = q.getAnswers().get(i);
                boolean isCorrect = q.getCorrectAnswerIndices().contains(i);
                System.out.println("  - " + ans + (isCorrect ? " (correct)" : ""));
            }
        }
    }

    private static void loginToMoodle(WebDriver driver, String username, String password) {
        driver.get("https://adskbtu.moodlecloud.com/login/index.php");

        WebElement usernameField = driver.findElement(By.id("username"));
        usernameField.sendKeys(username);

        WebElement passwordField = driver.findElement(By.id("password"));
        passwordField.sendKeys(password);

        WebElement loginButton = driver.findElement(By.id("loginbtn"));
        loginButton.click();
    }

    private static List<MoodleQuestion> scrapeMoodleQuiz(WebDriver driver) throws InterruptedException {
        driver.get("https://adskbtu.moodlecloud.com/mod/quiz/edit.php?cmid=41");

        List<MoodleQuestion> questions = new ArrayList<>();

        List<WebElement> questionBlocks = driver.findElements(
                By.cssSelector("li.activity div.activityinstance")
        );

        System.out.println("Found " + questionBlocks.size() + " question blocks.");

        for (WebElement block : questionBlocks) {
            try {
                MoodleQuestion q = new MoodleQuestion();

                // Question text (like "CHA9-E. WHICH OF THE FOLLOWING TYPES ARE ALL ...")
                WebElement textElem = block.findElement(By.cssSelector("span.questiontext"));
                q.setQuestionText(textElem.getText().trim());

                // Question type: often from an <img> or <i> with a title attribute. E.g.:
                //    <img class="icon activityicon" title="Multiple choice" src="...">
                try {
                    WebElement typeIcon = block.findElement(By.cssSelector("img.icon.activityicon"));
                    q.setQuestionType(typeIcon.getAttribute("title")); // e.g. "Multiple choice"
                } catch (NoSuchElementException e) {
                    q.setQuestionType("Unknown");
                }

                // Go to the Edit question page
                //    There's usually an <a title="Edit question Par3-OOP-9e-38" href="...">
                WebElement editLink = block.findElement(By.cssSelector("a[title^='Edit question ']"));
                String editQuestionUrl = editLink.getAttribute("href");

                // Open the question edit page (in the same tab for simplicity)
                driver.navigate().to(editQuestionUrl);
                Thread.sleep(2000);

                // Parse the answers & correct ones from the question edit form
                parseAnswersAndCorrectOnes(driver, q);

                // Go back to quiz-editing page
                driver.navigate().back();
                Thread.sleep(2000);

                questions.add(q);

            } catch (NoSuchElementException e) {
                System.out.println("Warning: Could not parse one question block. " + e.getMessage());
            }
        }
        return questions;
    }

    private static void parseAnswersAndCorrectOnes(WebDriver driver, MoodleQuestion q) {
        List<String> answers = new ArrayList<>();
        List<Integer> correctIndices = new ArrayList<>();

        // assume multiple choice format: answer[0], fraction[0], answer[1], fraction[1], ...
        // try indices from 0..10 (arbitrary) until we can't find them anymore.
        for (int i = 0; i < 10; i++) {
            try {
                // Text area for the answer text
                WebElement answerTextarea = driver.findElement(By.name("answer[" + i + "]"));
                String answerText = answerTextarea.getText().trim();

                // The fraction select indicates correctness: "1.0" or "0.5" or "0" for partial credit
//                WebElement fractionSelect = driver.findElement(By.name("fraction[" + i + "]"));
//                Select fractionDropdown = new Select(fractionSelect);
//                String fractionValue = fractionDropdown.getFirstSelectedOption().getAttribute("value");
//
//                answers.add(answerText);
//                if (!fractionValue.equals("0") && !fractionValue.equals("0.0")) {
//                    correctIndices.add(i);
//                }
            } catch (NoSuchElementException e) {
                // No more "answer[i]" found, so break.
                break;
            }
        }

        q.setAnswers(answers);
        q.setCorrectAnswerIndices(correctIndices);
    }

    private static WebElement findElementWithRetries(WebDriver driver, By locator, int maxRetries, long waitMillis) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                WebElement element = driver.findElement(locator);
                // Optionally, also check if element is displayed:
                // if (element.isDisplayed()) { return element; }
                return element;
            } catch (NoSuchElementException e) {
                System.out.println("Attempt " + attempt + ": Element not found (" + locator + "). Retrying...");
                try {
                    Thread.sleep(waitMillis);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Thread interrupted while waiting for element retry.", ie);
                }
            }
        }
        throw new NoSuchElementException(
                "Could not find element after " + maxRetries + " retries: " + locator.toString());
    }


    private static WebElement findElementNoThrow(WebDriver driver, By locator, int maxRetries, long waitMillis) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return driver.findElement(locator);
            } catch (NoSuchElementException e) {
                System.out.println("Attempt " + attempt + " to find " + locator + " failed. Retrying...");
                try {
                    Thread.sleep(waitMillis);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
        // If still not found
        return null;
    }

}
