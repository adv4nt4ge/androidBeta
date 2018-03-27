package test;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileBy;
import io.appium.java_client.MobileElement;
import io.appium.java_client.TouchAction;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.remote.AndroidMobileCapabilityType;
import io.appium.java_client.remote.MobileCapabilityType;
import org.apache.commons.io.IOUtils;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class TestGooglePlayApp {

    private static long INSTALL_DURATION_IN_SECONDS = 120L;
    final String appPackage = "com.murka.scatterslots";
    final String appActivity = "com.murka.android.core.MurkaUnityActivity";
    private AppiumDriver driver;
    private WebDriverWait wait;
    private long explicitWaitTimeoutInSeconds = 15L;

    @Given("^open Play Market$")
    public void setup() throws IOException, InterruptedException {
        DesiredCapabilities desiredCapabilities = new DesiredCapabilities();
        desiredCapabilities.setCapability(MobileCapabilityType.PLATFORM_NAME, "Android");
        desiredCapabilities.setCapability(MobileCapabilityType.PLATFORM_VERSION, appVersion());
        desiredCapabilities.setCapability(MobileCapabilityType.NO_RESET, true);
        desiredCapabilities.setCapability(MobileCapabilityType.DEVICE_NAME, nameDevice());
        desiredCapabilities.setCapability(AndroidMobileCapabilityType.APP_PACKAGE, "com.android.vending");
        desiredCapabilities.setCapability(AndroidMobileCapabilityType.APP_ACTIVITY, "com.google.android.finsky.activities.MainActivity");
        desiredCapabilities.setCapability("deviceOrientation", "portrait");
        driver = new AndroidDriver(new URL("http://0.0.0.0:4723/wd/hub"), desiredCapabilities);

        this.wait = new WebDriverWait(driver, explicitWaitTimeoutInSeconds);

        //проверка если приложение установленно то удалить его
        uninstallApp(appPackage);
    }


    @When("^input app name \"(.*)\"$")
    public void testGooglePlayApp(String testAppName) throws Exception {
        // ждем пока загрузится строка поиска в плей маркете
        wait.until(ExpectedConditions.visibilityOf(
                driver.findElement(MobileBy.AndroidUIAutomator("new UiSelector().resourceId(\"com.android.vending:id/search_box_idle_text\")"))))
                .click();

        // пишем имя приложения в поисковую строку
        driver.findElement(MobileBy.className("android.widget.EditText"))
                .sendKeys(testAppName);

        // тап для поиска нашего приложения и приводим все к маленьким буквам
        wait.until(ExpectedConditions.visibilityOf(
                driver.findElement(MobileBy.AndroidUIAutomator("new UiSelector()." +
                        "resourceId(\"com.android.vending:id/suggest_text\").text(\"" + testAppName.toLowerCase() + "\")"))))
                .click();
    }

    @When("^select app \"(.*)\"$")
    public void tapApp(String checkName) {
        // тап по нашему приложению в поиске
        wait.until(ExpectedConditions.visibilityOf(
                driver.findElement(MobileBy.AndroidUIAutomator("new UiSelector()." +
                        "resourceId(\"com.android.vending:id/li_title\").text(\"" + checkName + "\")")))).click();
    }

    @Then("^check app version \"(.*)\"$")
    public void checkVersion(String versionApp) {
        // проверка версии приложения
        driver.findElement(MobileBy.AndroidUIAutomator("new UiSelector().className(\"android.widget.TextView\")." +
                "resourceId(\"com.android.vending:id/footer_message\").text(\"ЧИТАТЬ ДАЛЬШЕ\")"))
                .click();

        wait.until(ExpectedConditions.visibilityOf(driver.findElement(MobileBy.AndroidUIAutomator
                ("new UiScrollable(new UiSelector()).scrollIntoView(new UiSelector()." +
                        "text(\"Адрес эл. почты разработчика\"));"))));

        try {
            wait.until(ExpectedConditions.visibilityOf(driver.findElement(MobileBy.AndroidUIAutomator("new UiSelector()." +
                    "className(\"android.widget.TextView\").resourceId(\"com.android.vending:id/extra_description\")." +
                    "text(\"" + versionApp.trim() + "\")"))));
        } catch (NoSuchElementException ex) {
            ex.getMessage();
            System.out.println("Version not found");
            driver.quit();
        }

        //закрыть доп инфо
        wait.until(ExpectedConditions.visibilityOf(driver.findElement(MobileBy.AndroidUIAutomator("new UiSelector()." +
                "className(\"android.widget.ImageButton\")"))))
                .click();
    }

    @When("^install app$")
    public void installApp() {
        // нажимаем кнопку установить
        wait.until(ExpectedConditions.visibilityOf(driver.findElement(MobileBy.AndroidUIAutomator("new UiSelector()." +
                "className(\"android.widget.Button\").text(\"УСТАНОВИТЬ\")"))))
                .click();

        // нажимаем подвердить если таковое окно появится
        try {
            MobileElement accept = (MobileElement) wait.until(ExpectedConditions.visibilityOf(driver.findElement(MobileBy.
                    AndroidUIAutomator("new UiSelector().resourceId(\"com.android.vending:id/continue_button\")"))));
            accept.click();
        } catch (NoSuchElementException pr) {
            pr.getMessage();
        }

        // ждем пока приложение установится
        new WebDriverWait(driver, INSTALL_DURATION_IN_SECONDS).until(ExpectedConditions.presenceOfElementLocated(
                MobileBy.AndroidUIAutomator("new UiSelector().className(\"android.widget.Button\").text(\"ОТКРЫТЬ\")")));

        // выход из плеймаркета
        driver.quit();
    }

    @Then("^start app$")
    public void startApp() throws Exception {
        // запуск приложения
        driver = new AndroidDriver(new URL("http://0.0.0.0:4723/wd/hub"), installedAppCaps());
        driver.launchApp();

        // подтвержедение со всеми доступами
        MobileElement buttonGranted = (MobileElement) wait.until(ExpectedConditions.visibilityOf(driver.findElement(MobileBy.
                AndroidUIAutomator("new UiSelector().className(\"android.widget.Button\").text(\"РАЗРЕШИТЬ\")"))));
        for (int i = 0; buttonGranted.isDisplayed(); i++) {
            buttonGranted.click();
        }

       /*wait.until(ExpectedConditions.visibilityOf(driver.findElement(MobileBy.
                AndroidUIAutomator("new UiSelector().resourceId(\"android:id/content\")"))));
*/
    }

    @When("^select position in buy menu$")
    public void buyPosition() throws InterruptedException {
        Thread.sleep(20000);
        TouchAction touchAction = new TouchAction(driver);
        Thread.sleep(3000);
        touchAction.tap(39, 512).perform();
        Thread.sleep(3000);
        touchAction.tap(39, 512).perform();

        //тап по кнопки buy
        Thread.sleep(3000);
        touchAction.tap(781, 50).perform();
        Thread.sleep(5000);
        //тап по кнопки
        touchAction.tap(1627, 817).perform();
        Thread.sleep(5000);

        // жмем кнопку купить
        MobileElement by = (MobileElement) wait.until(ExpectedConditions.visibilityOf(driver.findElement(MobileBy.
                AndroidUIAutomator("new UiSelector().className(\"android.widget.Button\").text(\"КУПИТЬ\")"))));
        by.click();

        try {
            if (!driver.findElement(MobileBy.
                    AndroidUIAutomator("new UiSelector().resourceId(\"com.android.vending:id/footer2\")")).isDisplayed()) {
                System.out.println("Error payment");
            }
        } catch (NoSuchElementException pr) {
            pr.getMessage();
            System.out.println("Error payment");
        }

    }

    public DesiredCapabilities installedAppCaps() throws Exception {

        DesiredCapabilities desiredCapabilities = new DesiredCapabilities();
        desiredCapabilities.setCapability(MobileCapabilityType.PLATFORM_NAME, "Android");
        desiredCapabilities.setCapability(MobileCapabilityType.PLATFORM_VERSION, appVersion());
        desiredCapabilities.setCapability(MobileCapabilityType.NO_RESET, true);
        desiredCapabilities.setCapability(MobileCapabilityType.DEVICE_NAME, nameDevice());
        desiredCapabilities.setCapability(AndroidMobileCapabilityType.APP_PACKAGE, appPackage);
        desiredCapabilities.setCapability(AndroidMobileCapabilityType.APP_ACTIVITY, appActivity);
        desiredCapabilities.setCapability("autoLaunch", "false");
        desiredCapabilities.setCapability("deviceOrientation", "portrait");
        return desiredCapabilities;
    }


    private String appVersion() throws IOException, InterruptedException {
        String cmd = "adb shell getprop ro.build.version.release";
        InputStream is = Runtime.getRuntime().exec(cmd).getInputStream();
        return IOUtils.toString(is, StandardCharsets.UTF_8);
    }

    private String nameDevice() throws IOException, InterruptedException {
        String cmd = "adb shell getprop ro.product.model";
        InputStream is = Runtime.getRuntime().exec(cmd).getInputStream();
        return IOUtils.toString(is, StandardCharsets.UTF_8);
    }

    private void uninstallApp(String appPackage) throws IOException, InterruptedException {
        final Process p = Runtime.getRuntime().exec("adb uninstall " + appPackage);

        new Thread(new Runnable() {
            public void run() {
                BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line = null;

                try {
                    while ((line = input.readLine()) != null)
                        System.out.println(line);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        p.waitFor();
    }
    /*@After
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }*/

}
