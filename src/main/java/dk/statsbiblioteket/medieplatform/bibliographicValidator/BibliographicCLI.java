package dk.statsbiblioteket.medieplatform.bibliographicValidator;

import dk.statsbiblioteket.util.xml.DOM;
import dk.statsbiblioteket.util.xml.XPathSelector;
import org.apache.commons.cli.*;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created with IntelliJ IDEA.
 * User: abr
 * Date: 10/9/12
 * Time: 4:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class BibliographicCLI {


    protected static final Option CHANNELID
            = new Option("channelID", true, "The sb channel id");
    protected static final Option STARTIIME
            = new Option("startTime", true, "The start time stamp");
    protected static final Option ENDTIME
            = new Option("endTime", true, "The end time stamp");

    protected static final Option FLUFF
            = new Option("fluff", true, "The maximum difference in durations");

    protected static final Option FFPROBE
            = new Option("ffprobe", true, "The ffprobe file");
    private static final Option CROSSCHECK
            = new Option("crosscheck", true, "The crosscheck file");

    private static Options options;
    private static DateFormat ourDateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmssZ");
    private  static long fluff;
    private static final SimpleDateFormat CROSSCHECK_TIME_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");

    public static void main(String... args) throws java.text.ParseException, FileNotFoundException, ParseException {

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        options = new Options();
        options.addOption(CHANNELID);
        options.addOption(STARTIIME);
        options.addOption(ENDTIME);
        options.addOption(FFPROBE);
        options.addOption(CROSSCHECK);
        options.addOption(FLUFF);

        for (Object option : options.getOptions()) {
            if (option instanceof Option) {
                ((Option) option).setRequired(true);
            }
        }

        cmd = parser.parse(options, args);

        //TODO use this for something?
        String temp = cmd.getOptionValue(CHANNELID.getOpt(),"");


        temp = cmd.getOptionValue(FLUFF.getOpt(),"1000");
        fluff = Long.parseLong(temp);
        temp = cmd.getOptionValue(STARTIIME.getOpt());
        Date startDate = ourDateFormat.parse(temp);

        temp = cmd.getOptionValue(ENDTIME.getOpt());
        Date endDate = ourDateFormat.parse(temp);
        long millisecondsExpected = (endDate.getTime() - startDate.getTime());


        temp = cmd.getOptionValue(FFPROBE.getOpt());
        Document doc = DOM.streamToDOM(new FileInputStream(temp),false);
        XPathSelector xpath = DOM.createXPathSelector("", "");
        // Do not use subtitle stream lengths, can't be trusted.
        NodeList durationsNodeList = xpath.selectNodeList(doc, "/ffprobe/streams/stream[@codec_type != 'subtitle']/@duration");
        boolean valid = true;

        long millisecondsActual = 0;
        for (int i = 0; i < durationsNodeList.getLength(); i++) {
            String durationString = durationsNodeList.item(i).getTextContent();
            millisecondsActual = Math.round(Double.parseDouble(durationString) * 1000);
            if (millisecondsActual+fluff <= millisecondsExpected || millisecondsActual-fluff >= millisecondsExpected) {
                valid = false;
                break;
            }
        }

        temp = cmd.getOptionValue(CROSSCHECK.getOpt());
        doc = DOM.streamToDOM(new FileInputStream(temp),false);
        xpath = DOM.createXPathSelector("", "");
        durationsNodeList = xpath.selectNodeList(doc, "/tsa-report/stream-information/duration/@value");

        CROSSCHECK_TIME_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        for (int i = 0; valid && i < durationsNodeList.getLength(); i++) {
            String durationString = durationsNodeList.item(i).getTextContent();
            millisecondsActual = CROSSCHECK_TIME_FORMAT.parse(durationString).getTime();
            if (millisecondsActual+fluff <= millisecondsExpected || millisecondsActual-fluff >= millisecondsExpected) {
                valid = false;
                break;
            }
        }

        if (valid){
            System.out.println("{\"valid\":true}");
        } else {
            System.out.println("File failed, duration from ffprobe or crosscheck = " + millisecondsActual + " but expected to be = " + millisecondsExpected);
            System.exit(1);
        }
    }



    protected static void parseError(String message) {
        System.err.println("Error parsing arguments");
        System.err.println(message);
        printUsage();
    }


    protected static void printUsage() {
        final HelpFormatter usageFormatter = new HelpFormatter();
        usageFormatter.printHelp(getHelpText(), options, true);
    }


    protected static String getHelpText() {
        return "packageForDoms";
    }
}


