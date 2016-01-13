package com.nibbler;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;


public class Main {

    private final static String PATH_TO_INI = "log_checker.ini";
    //private final static String PATH_TO_INI = "C:\\_replicator_checker\\log_checker.ini";

    private static String pathToLog = "";
    private static int statLogCount = 0;
    private static int statLogUpdateTime = 0;
    private static int timeForCheckingLines = 0;
    private static int timeCorrection = 0;
    private static int timeForNonUpdatedFile = 0;
    private static boolean needToSendLogFile = false;
    private static List<String> filesToCheck = new ArrayList<>();
    private static List<String> maskForFileToCheck = new ArrayList<>();
    private static List<String> errorCodes = new ArrayList<>();
    private static List<String> emailAddressesToNotify = new ArrayList<>();
    private static List<String> smtpSettings = new ArrayList<>();
    private static List<String> resultMessage = new ArrayList<>();
    private static List<String> filesToIgnore = new ArrayList<>();
    private static List<String> linesToIgnore = new ArrayList<>();
    private static List<String> filesTimeUpdateToIgnore = new ArrayList<>();


    public static void main(String[] args) {
        System.out.println("Log checker start...");
        File logFile = new File("log.txt");
        if (logFile.exists()) logFile.delete();
        writeToLog("Log checker start...");

        if (!loadSettingsFromIni()) {
            sendEmail();
            return;
        }

        DateFormat dateFormat = new SimpleDateFormat("yy-MM-dd");
        Date date = new Date();
        String todayFolderToCheck = dateFormat.format(date);
        System.out.println("Current day: " + todayFolderToCheck);
        writeToLog("Current day: " + todayFolderToCheck);

        dateFormat = new SimpleDateFormat("HH:mm:ss");
        String currentTime = dateFormat.format(date);
        System.out.println("Current time: " + currentTime);
        writeToLog("Current time: " + currentTime);

        File file = new File(pathToLog + todayFolderToCheck);
        if (file.exists() && file.isDirectory()) {
            System.out.println("Folder for checking exists: " + file.getAbsolutePath());
            writeToLog("Folder for checking exists: " + file.getAbsolutePath());
            File[] listOfFiles = file.listFiles();

            if (listOfFiles != null && listOfFiles.length > 0) {
                checkDirectoryForFilesQuantity(listOfFiles);
                checkFileUpdateTime(listOfFiles);
                checkFileForError(listOfFiles);
            }

            if (resultMessage.size() > 0 || needToSendLogFile) {
                if (sendEmail()) {
                    System.out.println("The email has successfully sent");
                    writeToLog("The email has successfully sent");
                } else {
                    System.out.println("ATTENTION! The email hasn't been sent!");
                    writeToLog("ATTENTION! The email hasn't been sent!");
                }
            }
        } else {
            System.out.println("ATTENTION! Don't exist: " + file.getAbsolutePath());
            writeToLog("ATTENTION! Don't exist: " + file.getAbsolutePath());
            sendEmail();
        }
    }

    private static void writeToLog(String stringToLog) {
        BufferedWriter writer = null;
        try {
            File logFile = new File("log.txt");
            writer = new BufferedWriter(new FileWriter(logFile, true));
            writer.write(stringToLog + "\r\n");
            if (stringToLog.toLowerCase().contains("attention") || stringToLog.toLowerCase().contains("exception")) needToSendLogFile = true;
        } catch (Exception e) {
            System.out.println("ATTENTION! writeToLog Exception");
            writeToLog("ATTENTION! writeToLog Exception");
        } finally {
            try {
                writer.close();
            } catch (Exception e) {
                System.out.println("ATTENTION! writeToLog writer.close Exception");
                writeToLog("ATTENTION! writeToLog writer.close Exception");
            }
        }

    }

    private static boolean sendEmail() {
        System.out.println("Trying to send an email");
        writeToLog("Trying to send an email");
        String host = "";
        String from = "";
        //String pass = "";
        //String port = "";

        for (int i = 0; i < smtpSettings.size(); i++) {
            String tmp = smtpSettings.get(i);
            if (tmp.contains("smtp_server_address=")) host = tmp.substring(tmp.indexOf("=") + 1, tmp.length());
            //if (tmp.contains("smtp_port="))  port = tmp.substring(tmp.indexOf("=") + 1, tmp.length());
            if (tmp.contains("smtp_login="))  from = tmp.substring(tmp.indexOf("=") + 1, tmp.length());
            //if (tmp.contains("smtp_password="))  pass = tmp.substring(tmp.indexOf("=") + 1, tmp.length());
        }

        if (host.equals("")) host = "172.16.6.6";
        if (from.equals("")) from = "replicator_checker@7828882.ru";
        if (emailAddressesToNotify.size() == 0) {
            emailAddressesToNotify.add("gusev@7828882.ru");
            emailAddressesToNotify.add("nn-admin@nnkk.budzdorov.su");
        }

        System.out.println("host: " + host);
        writeToLog("host: " + host);
        System.out.println("from: " + from);
        writeToLog("from: " + from);
        /*System.out.println("pass: " + pass);
        writeToLog("pass: " + pass);
        System.out.println("port: " + port);
        writeToLog("port: " + port);*/

        Properties properties = System.getProperties();
        properties.setProperty("mail.smtp.host", host);
        //properties.setProperty("mail.user", from);
        //properties.setProperty("mail.password", pass);
        //properties.setProperty("mail.smtp.port", port);
        Session session = Session.getDefaultInstance(properties);
        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));

            InternetAddress[] internetAddresses = new InternetAddress[emailAddressesToNotify.size()];
            for (int i = 0; i < emailAddressesToNotify.size(); i++) {
                System.out.println(emailAddressesToNotify.get(i));
                writeToLog(emailAddressesToNotify.get(i));
                internetAddresses[i] = new InternetAddress(emailAddressesToNotify.get(i));
            }
            message.addRecipients(Message.RecipientType.TO, internetAddresses);

            if (needToSendLogFile) {
                MimeMessage message1 = message;
                message1.setSubject("ATTENTION! Something wrong in the replicator checker work");
                BodyPart messageBodyPart = new MimeBodyPart();
                messageBodyPart.setText("Replicator checker log");
                Multipart multipart = new MimeMultipart();
                multipart.addBodyPart(messageBodyPart);
                messageBodyPart = new MimeBodyPart();
                DataSource source = new FileDataSource("log.txt");
                messageBodyPart.setDataHandler(new DataHandler(source));
                messageBodyPart.setFileName("log.txt");
                multipart.addBodyPart(messageBodyPart);
                message1.setContent(multipart);
                Transport.send(message1);
            }

            if (resultMessage.size() > 0) {
                StringBuilder stringBuilder = new StringBuilder();
                String resultText = "";
                for (String s : resultMessage) {
                    stringBuilder.append(resultText).append(s).append("<br/>");
                }
                message.setContent(stringBuilder.toString(), "text/html");
                message.setSubject("WARNING! There are some errors in the replicator work");
                Transport.send(message);
            }

            return true;
        } catch (MessagingException e) {
            System.out.println("ATTENTION! sendEmail MessagingException");
            writeToLog("ATTENTION! sendEmail MessagingException");
        }

        return false;
    }

    private static void checkFileForError(File[] listOfFiles) {
        boolean isThereAreSomeErrors = false;

        for (int i = 0; i < listOfFiles.length; i++) {
            if (!listOfFiles[i].isFile()) continue;
            File file = listOfFiles[i];
            String fileName = file.getName();

            boolean needToIgnore = false;
            for (String ignore : filesToIgnore) {
                if (fileName.contains(ignore)) needToIgnore = true;
            }
            if (needToIgnore) {
                System.out.println("checkFileForError skipping the file: " + fileName);
                writeToLog("checkFileForError skipping the file: " + fileName);
                continue;
            }

            for (String name : maskForFileToCheck) {
                if (!fileName.contains(name)) continue;
                System.out.println("Checking for the error in the file: " + file.getName());
                writeToLog("Checking for the error in the file: " + file.getName());
                try {
                    BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
                    String line;
                    DateFormat dateFormatLine = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                    DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
                    Date date = new Date();
                    Date dateLine = null;
                    String today = dateFormat.format(date);
                    boolean isAnyLineToCheck = false;
                    while ((line = bufferedReader.readLine()) != null) {
                        if (line.equals("")) continue;
                        if (!line.contains(today) && !line.contains(":")) continue;
                        try {
                            if (line.length() > 19) dateLine = dateFormatLine.parse(line.substring(0, 19));
                            if (dateLine == null) continue;
                            long timeInLine = TimeUnit.MILLISECONDS.toMinutes(date.getTime() - dateLine.getTime());
                            if (!file.getName().contains("stat") && timeInLine > timeForCheckingLines + timeCorrection) continue;
                            if (file.getName().contains("stat") && timeInLine > statLogUpdateTime + timeCorrection) continue;
                            boolean isThereAreAnyError = false;
                            while ((line = bufferedReader.readLine()) != null) {
                                if (line.equals("")) continue;
                                isAnyLineToCheck = true;
                                if (file.getName().contains("stat_")) {
                                    if (!line.toLowerCase().contains("count")) continue;
                                    int whereToFind = line.lastIndexOf(": ");
                                    int statCount = Integer.parseInt(line.substring(whereToFind + 2, line.length()));
                                    if (statCount < 1) {
                                        resultMessage.add("<b><i>Cannot resolve the tasks count in the file: " + file.getName() + "</i></b>");
                                        System.out.println("WARNING! Wrong tasks count in the line: " + line);
                                        writeToLog("WARNING! Wrong tasks count in the line: " + line);
                                    } else {
                                        if (statCount == statLogCount) continue;
                                        resultMessage.add("<b><i>WARNING! Tasks count in file: " + file.getName() + " (" +
                                                statCount + ") doesn't equal the counter in the settings file: " + statLogCount + "</i></b>");
                                        System.out.println("WARNING! Tasks count in file: " + file.getName() + " (" +
                                                statCount + ") doesn't equal the counter in the settings file: " + statLogCount);
                                        writeToLog("WARNING! Tasks count in file: " + file.getName() + " (" +
                                                statCount + ") doesn't equal the counter in the settings file: " + statLogCount);
                                    }
                                } else {
                                    for (String ignore : linesToIgnore) {
                                        try {
                                            if (line.contains("===>") && line.contains(ignore)) {
                                                while ((line = bufferedReader.readLine()) != null) {
                                                    if (line.equals("")) continue;
                                                    if (line.contains("===>")) {
                                                        break;
                                                    }
                                                }
                                            }

                                            if (line.contains("--->") && line.contains(ignore)) {
                                                while ((line = bufferedReader.readLine()) != null) {
                                                    if (line.equals("")) continue;
                                                    if (line.contains("--->")) {
                                                        break;
                                                    }
                                                }
                                            }
                                        } catch (NullPointerException e) {
                                            //System.out.println("___NullPointerException");
                                        }
                                    }

                                    if (line == null) continue;

                                    for (int x = 0; x < errorCodes.size(); x++) {

                                        try {
                                            if (!line.toLowerCase().contains(errorCodes.get(x))) continue;
                                        } catch (NullPointerException e) {
                                            System.out.println("NullPointerException 2");
                                        }

                                        if (!isThereAreAnyError) {
                                            if (!isThereAreSomeErrors) resultMessage.add("<b>Files that contains some errors:</b>");
                                            isThereAreSomeErrors = true;
                                            resultMessage.add("<b><i>" + file.getName() + "</i></b>");
                                            isThereAreAnyError = true;
                                        }
                                        resultMessage.add(line);
                                        System.out.println(line);
                                        writeToLog(line);
                                    }
                                }
                            }
                        } catch (ParseException e) {
                            //System.out.println("checkFileForError ParseException: " + line);
                        }
                    }
                    if (!isAnyLineToCheck) {
                        System.out.println("In the file: " + fileName + " no entries which time creation are less than " + timeForCheckingLines + " minutes");
                        writeToLog("In the file: " + fileName + " no entries which time creation are less than " + timeForCheckingLines + " minutes");
                        long lastUpdateTime = TimeUnit.MILLISECONDS.toMinutes(date.getTime() - listOfFiles[i].lastModified());
                        System.out.println("Last time update: " + lastUpdateTime + " minutes ago");
                        if (lastUpdateTime < timeForCheckingLines) {
                            System.out.println("ATTENTION! There are some problems with time zones");
                            writeToLog("ATTENTION! There are some problems with time zones");
                        }
                    }
                } catch (IOException e) {
                    System.out.println("ATTENTION! checkFileForError IOException: " + file.getName());
                    writeToLog("ATTENTION! checkFileForError IOException: " + file.getName());
                }
            }
        }
        if (isThereAreSomeErrors) resultMessage.add("<br>");
        if (!isThereAreSomeErrors) {
            System.out.println("There are no files that contain any error from the list");
            writeToLog("There are no files that contain any error from the list");
        }
    }

    private static void checkFileUpdateTime(File[] listOfFiles) {
        Date date = new Date();
        boolean isThereAreSomeErrors = false;

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()){

                boolean needToIgnore = false;
                String fileName = listOfFiles[i].getName();

                for (String ignore : filesToIgnore) {
                    if (fileName.contains(ignore)) needToIgnore = true;
                }

                for (String ignore : filesTimeUpdateToIgnore) {
                    if (fileName.contains(ignore)) needToIgnore = true;
                }

                if (needToIgnore) {
                    System.out.println("checkFileUpdateTime skipping the file: " + fileName);
                    writeToLog("checkFileUpdateTime skipping the file: " + fileName);
                    continue;
                }

                for (String name : maskForFileToCheck) {
                    if (fileName.contains(name)) {
                        long lastUpdateTime = TimeUnit.MILLISECONDS.toMinutes(date.getTime() - listOfFiles[i].lastModified());
                        if (lastUpdateTime > timeForNonUpdatedFile) {
                            if (!isThereAreSomeErrors) resultMessage.add("<b>Files that has been updated more than " + timeForNonUpdatedFile + " minutes ago:</b>");
                            isThereAreSomeErrors = true;
                            resultMessage.add(listOfFiles[i].getName() + " has been updated " + lastUpdateTime + " minutes ago");
                            System.out.println(listOfFiles[i].getName() + " has been updated " + lastUpdateTime + " minutes ago");
                            writeToLog(listOfFiles[i].getName() + " has been updated " + lastUpdateTime + " minutes ago");
                        }
                    }
                }
            }
        }
        if (isThereAreSomeErrors) resultMessage.add("<br>");
        if (!isThereAreSomeErrors) {
            System.out.println("All files are successfully updated less than " + timeForNonUpdatedFile + " minutes ago");
            writeToLog("All files are successfully updated less than " + timeForNonUpdatedFile + " minutes ago");
        }
    }

    private static void checkDirectoryForFilesQuantity(File[] files) {
        boolean isThereAreAnyError = false;
        for (int i = 0; i < filesToCheck.size(); i++) {
            boolean fileFound = false;
            for (int x = 0; x < files.length; x++) {
                if (files[x].getName().toLowerCase().contains(filesToCheck.get(i).toLowerCase())) fileFound = true;
            }
            if (!fileFound) {
                if (!isThereAreAnyError) resultMessage.add("<b>Files that doesn't exists:</b>");
                isThereAreAnyError = true;
                resultMessage.add(filesToCheck.get(i));
                System.out.println("WARNING! The folder doesn't contain file: " + filesToCheck.get(i));
                writeToLog("WARNING! The folder doesn't contain file: " + filesToCheck.get(i));
            }
        }
        if (isThereAreAnyError) resultMessage.add("<br>");
        if (!isThereAreAnyError) {
            System.out.println("All files for checking exists");
            writeToLog("All files for checking exists");
        }

        isThereAreAnyError = false;
        for (int i = 0; i < files.length; i++) {
            boolean needToIgnore = false;
            for (String ignore : filesToIgnore) {
                if (files[i].getName().toLowerCase().contains(ignore)) needToIgnore = true;
            }
            if (needToIgnore) {
                System.out.println("checkDirectoryForFilesQuantity skipping the file: " + files[i].getName());
                writeToLog("checkDirectoryForFilesQuantity skipping the file: " + files[i].getName());
                continue;
            }


            boolean fileFound = false;
            for (int x = 0; x < filesToCheck.size(); x++) {
                if (files[i].getName().toLowerCase().contains(filesToCheck.get(x).toLowerCase())) fileFound = true;
            }
            if (!fileFound) {
                for (String mask : maskForFileToCheck) {
                    if (files[i].getName().toLowerCase().contains(mask)) {
                        if (!isThereAreAnyError) resultMessage.add("<b>Files that exists but not presented in the settings file:</b>");
                        isThereAreAnyError = true;
                        resultMessage.add(files[i].getName());
                        System.out.println("WARNING! The file: " + files[i].getName() + " exist in folder but doesn't present in the settings file");
                        writeToLog("WARNING! The file: " + files[i].getName() + " exist in folder but doesn't present in the settings file");
                    }
                }
            }
        }
        if (isThereAreAnyError) resultMessage.add("<br>");
        if (!isThereAreAnyError) {
            System.out.println("There are no extra files in folder");
            writeToLog("There are no extra files in folder");
        }
    }

    private static boolean loadSettingsFromIni(){
        File file = new File(PATH_TO_INI);
        if (file.exists()) {
            System.out.println("Parsing the settings file: " + PATH_TO_INI);
            writeToLog("Parsing the settings file: " + PATH_TO_INI);

            try {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
                String line;
                int counter = 0;
                boolean foundSomething = false;
                while ((line = bufferedReader.readLine()) != null) {
                    if (line.equals("===PathLog===")) {
                        while ((line = bufferedReader.readLine()) != null) {
                            if (!line.equals("===EndPathLog===")) {
                                if (!line.equals("")) pathToLog = line;
                                foundSomething = true;
                            } else {
                                if (foundSomething) counter += 1;
                                break;
                            }
                        }
                        System.out.println("pathToLog: " + pathToLog);
                        writeToLog("pathToLog: " + pathToLog);
                    }

                    foundSomething = false;
                    if (line != null && line.equals("===FilesToCheck===")) {
                        while ((line = bufferedReader.readLine()) != null) {
                            if (!line.equals("===EndFilesToCheck===")) {
                                if (!line.equals("")) filesToCheck.add(line);
                                foundSomething = true;
                            } else {
                                if (foundSomething) counter += 1;
                                break;
                            }
                        }
                        System.out.println("filesToCheck.size: " + filesToCheck.size());
                        writeToLog("filesToCheck.size: " + filesToCheck.size());
                    }

                    foundSomething = false;
                    if (line != null && line.equals("===GeneralSettings===")) {
                        while ((line = bufferedReader.readLine()) != null) {
                            if (!line.equals("===EndGeneralSettings===")) {
                                if (!line.equals("") && line.contains("statLogCount=")) {
                                    statLogCount = Integer.parseInt(line.substring(line.indexOf("=") + 1, line.length()));
                                    System.out.println("statLogCount: " + statLogCount);
                                    writeToLog("statLogCount: " + statLogCount);
                                }

                                if (!line.equals("") && line.contains("statLogUpdateTime=")) {
                                    statLogUpdateTime = Integer.parseInt(line.substring(line.indexOf("=") + 1, line.length()));
                                    System.out.println("statLogUpdateTime: " + statLogUpdateTime);
                                    writeToLog("statLogUpdateTime: " + statLogUpdateTime);
                                }

                                if (!line.equals("") && line.contains("timeForCheckingLines=")) {
                                    timeForCheckingLines = Integer.parseInt(line.substring(line.indexOf("=") + 1, line.length()));
                                    System.out.println("timeForCheckingLines: " + timeForCheckingLines);
                                    writeToLog("timeForCheckingLines: " + timeForCheckingLines);
                                }

                                if (!line.equals("") && line.contains("timeCorrection=")) {
                                    timeCorrection = Integer.parseInt(line.substring(line.indexOf("=") + 1, line.length()));
                                    System.out.println("timeCorrection: " + timeCorrection);
                                    writeToLog("timeCorrection: " + timeCorrection);
                                }

                                if (!line.equals("") && line.contains("timeForNonUpdatedFile=")) {
                                    timeForNonUpdatedFile = Integer.parseInt(line.substring(line.indexOf("=") + 1, line.length()));
                                    System.out.println("timeForNonUpdatedFile: " + timeForNonUpdatedFile);
                                    writeToLog("timeForNonUpdatedFile: " + timeForNonUpdatedFile);
                                }
                               foundSomething = true;
                            } else {
                                if (foundSomething) counter += 1;
                                break;
                            }
                        }
                    }

                    foundSomething = false;
                    if (line!= null && line.equals("===ErrorCodes===")) {
                        while ((line = bufferedReader.readLine()) != null) {
                            if (!line.equals("===EndErrorCodes===")) {
                                if (!line.equals("")) errorCodes.add(line);
                                foundSomething = true;
                            } else {
                                if (foundSomething) counter += 1;
                                break;
                            }
                        }
                        System.out.println("errorCodes.size: " + errorCodes.size());
                        writeToLog("errorCodes.size: " + errorCodes.size());
                    }

                    foundSomething = false;
                    if (line != null && line.equals("===EmailAddressesToNotify===")) {
                        while ((line = bufferedReader.readLine()) != null) {
                            if (!line.equals("===EndEmailAddressesToNotify===")) {
                                if (!line.equals("")) emailAddressesToNotify.add(line);
                                foundSomething = true;
                            } else {
                                if (foundSomething) counter += 1;
                                break;
                            }
                        }
                        System.out.println("emailAddressesToNotify.size: " + emailAddressesToNotify.size());
                        writeToLog("emailAddressesToNotify.size: " + emailAddressesToNotify.size());
                    }

                    foundSomething = false;
                    if (line != null && line.equals("===SmtpSettings===")) {
                        while ((line = bufferedReader.readLine()) != null) {
                            if (!line.equals("===EndSmtpSettings===")) {
                                if (!line.equals("")) smtpSettings.add(line);
                                foundSomething = true;
                            } else {
                                if (foundSomething) counter += 1;
                                break;
                            }
                        }
                        System.out.println("smtpSettings.size: " + smtpSettings.size());
                        writeToLog("smtpSettings.size: " + smtpSettings.size());
                    }

                    foundSomething = false;
                    if (line != null && line.equals("===MaskForFileToCheck===")) {
                        while ((line = bufferedReader.readLine()) != null) {
                            if (!line.equals("===EndMaskForFileToCheck===")) {
                                if (!line.equals("")) maskForFileToCheck.add(line);
                                foundSomething = true;
                            } else {
                                if (foundSomething) counter += 1;
                                break;
                            }
                        }
                        System.out.println("maskForFileToCheck.size: " + maskForFileToCheck.size());
                        writeToLog("maskForFileToCheck.size: " + maskForFileToCheck.size());
                    }

                    if (line != null && line.equals("===FilesToIgnore===")) {
                        while ((line = bufferedReader.readLine()) != null) {
                            if (!line.equals("===EndFilesToIgnore===")) {
                                if (!line.equals("")) filesToIgnore.add(line);
                            } else {
                                break;
                            }
                        }
                        System.out.println("filesToIgnore.size: " + filesToIgnore.size());
                        writeToLog("filesToIgnore.size: " + filesToIgnore.size());
                    }

                    if (line != null && line.equals("===LinesToIgnore===")) {
                        while ((line = bufferedReader.readLine()) != null) {
                            if (!line.equals("===EndLinesToIgnore===")) {
                                if (!line.equals("")) linesToIgnore.add(line);
                            } else {
                                break;
                            }
                        }
                        System.out.println("linesToIgnore.size: " + linesToIgnore.size());
                        writeToLog("linesToIgnore.size: " + linesToIgnore.size());
                    }

                    if (line != null && line.equals("===FileTimeUpdateToIgnore===")) {
                        while ((line = bufferedReader.readLine()) != null) {
                            if (!line.equals("===EndFileTimeUpdateToIgnore===")) {
                                if (!line.equals("")) filesTimeUpdateToIgnore.add(line);
                            } else {
                                break;
                            }
                        }
                        System.out.println("filesTimeUpdateToIgnore.size: " + filesTimeUpdateToIgnore.size());
                        writeToLog("filesTimeUpdateToIgnore.size: " + filesTimeUpdateToIgnore.size());
                    }
                }

                System.out.println("Counter: " + counter);

                if (counter < 7) {
                    System.out.println("ATTENTION! Something wrong with settings file");
                    writeToLog("ATTENTION! Something wrong with settings file");
                    return false;
                } else {
                    System.out.println("Settings are loaded correctly");
                    writeToLog("Settings are loaded correctly");
                    return true;
                }

            } catch (FileNotFoundException e) {
                System.out.println("ATTENTION! loadSettingsFromIni FileNotFoundException: " + PATH_TO_INI);
                writeToLog("ATTENTION! loadSettingsFromIni FileNotFoundException: " + PATH_TO_INI);
            } catch (IOException e) {
                System.out.println("ATTENTION! loadSettingsFromIni IOException: " + PATH_TO_INI);
                writeToLog("ATTENTION! loadSettingsFromIni IOException: " + PATH_TO_INI);
            } catch (NullPointerException e) {
                System.out.println("ATTENTION! loadSettingsFromIni NullPointerException: " + PATH_TO_INI);
                writeToLog("ATTENTION! loadSettingsFromIni NullPointerException: " + PATH_TO_INI);
            }

        } else {
            System.out.println("ATTENTION! The settings file doesn't exist at path: " + PATH_TO_INI);
            writeToLog("ATTENTION! The settings file doesn't exist at path: " + PATH_TO_INI);
        }
        return false;
    }
}
