package com.ibs.LogEditor;

import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Vector;

public class Main extends JFrame{
    JFileChooser file_chooser = null;
    File log_file = null;
    Vector<String> file_names;
    String line;
    FileReader file_reader = null;
    BufferedReader reader = null;
    Boolean isXML = false;
    Boolean isJSON = false;

    Main() {
        file_names = new Vector<>();
        fileChoosing();
        fileReading();
        if(isXML)
            fileReadingXML();
        if(isJSON)
            fileReadingJSON();
        fileRenaming();
        if(isXML)
            fileCorrelationXML();
        if(isJSON)
            fileCorrelationJSON();
    }

    public static void main(String[] args) {
        Main main = new Main();
    }

    public void fileChoosing(){
        file_chooser = new JFileChooser(System.getProperty("user.dir"));
        int result = file_chooser.showDialog(null, "Открыть файл");
        if(result == JFileChooser.APPROVE_OPTION)
            log_file = file_chooser.getSelectedFile();
    }

    public void fileReading(){
        try {
            file_reader = new FileReader(log_file);
            reader = new BufferedReader(file_reader);
            line = reader.readLine();
            while(line != null) {
                if (line.contains("printRequest") && line.contains("<map>")) {
                    isXML = true;
                    break;
                }
                if(line.contains("printRequest") && !line.contains("<map>")){
                    isJSON = true;
                    break;
                }
                line = reader.readLine();
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    public void fileReadingXML() {
        Boolean writing_into_file = false;
        Boolean changing_filename = false;
        File single_operation_file_xml;
        FileWriter single_operation_file_writer_xml = null;
        String single_operation_filename_xml;
        int i = 1;
        int begin_map = 0;
        int end_map = 0;

        try {
            while (line != null) {
                if (line.contains("printRequest")) {
                    writing_into_file = true;
                    changing_filename = true;
                    single_operation_file_xml = new File(log_file.getParent() + File.separator + i + ".txt");
                    single_operation_file_writer_xml = new FileWriter(single_operation_file_xml, true);
                    single_operation_file_writer_xml.write("<map>");
                    begin_map++;
                    i++;
                    single_operation_file_writer_xml.flush();
                    line = reader.readLine();
                }
                if ((begin_map == end_map) && (begin_map != 0)) {
                    writing_into_file = false;
                    begin_map = 0;
                    end_map = 0;
                    single_operation_file_writer_xml.close();
                }
                if (writing_into_file) {
                    if (line.contains("<map>"))
                        begin_map++;
                    if (line.contains("</map>"))
                        end_map++;
                    single_operation_file_writer_xml.write(line);
                    single_operation_file_writer_xml.flush();
                }
                if (changing_filename)
                    if (line.contains("ru.vtb24.mobilebanking.protocol.operation.")) {
                        single_operation_filename_xml = StringUtils.substringBetween(line, "<type>ru.vtb24.mobilebanking.protocol.operation.", "</type>");
                        if(single_operation_filename_xml != null)
                            file_names.add((i - 1) + ". " + single_operation_filename_xml + ".txt");
                        changing_filename = false;
                    }
                line = reader.readLine();
            }
            reader.close();
            file_reader.close();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    public void fileReadingJSON(){
        File single_operation_file_json;
        FileWriter single_operation_file_writer_json;
        String single_operation_filename_json;
        int i = 1;

        try {
            while (line != null) {
                if (line.contains("printRequest")){
                    line = line.substring(line.indexOf('{'));
                    single_operation_file_json = new File(log_file.getParent() + File.separator + i + ".txt");
                    single_operation_file_writer_json = new FileWriter(single_operation_file_json, true);
                    single_operation_file_writer_json.write(line);
                    single_operation_file_writer_json.flush();
                    if (line.contains("ru.vtb24.mobilebanking.protocol.minervadirect.")) {
                        single_operation_filename_json = StringUtils.substringBetween(line, "{\"ru.vtb24.mobilebanking.protocol.minervadirect.", "\"");
                        if(single_operation_file_json != null)
                            file_names.add(i + ". " + single_operation_filename_json + ".txt");
                        i++;
                        single_operation_file_writer_json.close();
                        continue;
                    }
                    if (line.contains("ru.vtb24.mobilebanking.protocol.operation.")) {
                        single_operation_filename_json = StringUtils.substringBetween(line, "{\"ru.vtb24.mobilebanking.protocol.operation.", "\"");
                        if(single_operation_file_json != null)
                            file_names.add(i + ". " + single_operation_filename_json + ".txt");
                        i++;
                        single_operation_file_writer_json.close();
                        continue;
                    }
                }
                line = reader.readLine();
            }
            reader.close();
            file_reader.close();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }

    }

    public void fileRenaming(){
        Path source;
        for(int i = 0; i < file_names.size(); i++) {
            source = Paths.get(log_file.getParent() + File.separator + (i + 1) + ".txt");
            try {
                Files.move(source, source.resolveSibling(file_names.elementAt(i)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void fileCorrelationXML(){
        FileReader file_reader;
        FileWriter file_writer;
        BufferedReader reader;
        String line;

        for(int i = 0; i < file_names.size(); i++){
            try {
                file_reader = new FileReader(log_file.getParent() + File.separator + file_names.elementAt(i));
                reader = new BufferedReader(file_reader);
                line = reader.readLine();

                line = line.replaceAll(" ", "");
                line = line.replaceAll("<string/>", "<string></string>");
                line = line.replaceAll("<type/>", "<type></type>");
                line = line.replaceAll("<null/>", "<null></null>");

                String sid = StringUtils.substringBetween(line, "<string>CLIENT-TOKEN</string><string>", "</string>");
                if(sid != null)
                    line = line.replaceAll(sid, "{sid}");

                String timestamp = StringUtils.substringBetween(line, "\"TIMESTAMP\":", ",");
                if(timestamp != null)
                    line = line.replaceAll(timestamp, "{timeStamp}");

                timestamp = StringUtils.substringBetween(line, "\"Timestamp\":", ",");
                if(timestamp != null)
                    line = line.replaceAll(timestamp, "{timeStamp}");

                timestamp = StringUtils.substringBetween(line, "<string>sendTimestamp</string><long>", "</long>");
                if(timestamp != null)
                    line = line.replaceAll(timestamp, "{timeStamp}");

                String login = StringUtils.substringBetween(line, "<string>USER_ID</string><string>", "</string>");
                if(login != null)
                    line = line.replaceAll(login, "{login}");

                String app_Ver = StringUtils.substringBetween(line, "<string>APP_VERSION</string><string>", "</string>");
                if(app_Ver != null)
                    line = line.replaceAll(app_Ver, "{app_Ver}");

                line = line.replaceAll("\"", "\\\\\"");
                line = "\\\\x00\\\\x00{len0}\\\\x06{Protocol_Atr}" + line;

                reader.close();

                file_writer = new FileWriter(log_file.getParent() + File.separator + file_names.elementAt(i), false);
                file_writer.write(line);
                file_writer.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void fileCorrelationJSON(){
        FileReader file_reader;
        FileWriter file_writer;
        BufferedReader reader;
        String line;

        for(int i = 0; i < file_names.size(); i++){
            try {
                file_reader = new FileReader(log_file.getParent() + File.separator + file_names.elementAt(i));
                reader = new BufferedReader(file_reader);
                line = reader.readLine();

                String sid = StringUtils.substringBetween(line, "\"CLIENT-TOKEN\":\"", "\"");
                if(sid != null)
                    line = line.replaceAll(sid, "{sid}");

                String timestamp = StringUtils.substringBetween(line, "\\\"TIMESTAMP\\\":", ",");
                if(timestamp != null)
                    line = line.replaceAll(timestamp, "{timeStamp}");

                timestamp = StringUtils.substringBetween(line, "\"sendTimestamp\":", ",");
                if(timestamp != null)
                    line = line.replaceAll(timestamp, "{timeStamp}");

                String login = StringUtils.substringBetween(line, "\"USER_ID\":\"", "\"");
                if(login != null)
                    line = line.replaceAll(login, "{login}");

                String app_Ver = StringUtils.substringBetween(line, "\"APP_VERSION\":\"", "\"");
                if(app_Ver != null)
                    line = line.replaceAll(app_Ver, "{app_Ver}");

                String protocol_Atr = StringUtils.substringBetween(line, "\"PROTOVERSION\":\"", "\"");
                if(app_Ver != null)
                    line = line.replaceAll(protocol_Atr, "{Protocol_Atr}");

                line = line.replaceAll("\"", "\\\\\"");
                line = line.replaceAll("\\\\\\\\\"", "\\\\\\\\\\\\\"");
                reader.close();

                file_writer = new FileWriter(log_file.getParent() + File.separator + file_names.elementAt(i), false);
                file_writer.write(line);
                file_writer.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}