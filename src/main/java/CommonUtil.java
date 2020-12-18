import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author huilong.ning
 * @date October 18, 2019 Description: Public tools
 */
public class CommonUtil {
    /**
     * Get stack information
     *
     * @return
     */
    public static String getStackInfo(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        // Output error stack information to print Writer
        e.printStackTrace(pw);
        pw.flush();
        sw.flush();
        if (sw != null) {
            try {
                sw.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        if (pw != null) {
            pw.close();
        }
        return sw.toString();
    }

}
