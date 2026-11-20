package nablarch.test;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import nablarch.core.date.SystemTimeProvider;
import nablarch.core.util.StringUtil;
import nablarch.core.util.annotation.Published;

/**
 * 固定日時を提供するクラス。
 * <br>
 * 固定日時は{@link #setFixedDate(String)}で指定する。
 *
 * @author Kiyohito Itoh
 */
@Published(tag = "architect")
public class FixedSystemTimeProvider implements SystemTimeProvider {
    
    /** 日時を解釈するためのフォーマット（最短）*/
    private static final String SHORTEST_FORMAT = "yyyyMMddHHmmss";
    
    /** 日時を解釈するためのフォーマット（最長）*/
    private static final String LONGEST_FORMAT = SHORTEST_FORMAT + "SSS";
    
    /** 固定日時 */
    private Date fixedDate;

    /**
     * 固定日時を設定する。<br/>
     * 引数には以下のフォーマットいずれかに合致する文字列を指定すること。
     * <ul>
     * <li>yyyyMMddHHmmss (14桁)</li>
     * <li>yyyyMMddHHmmssSSS (17桁)</li>
     * </ul>
     * yyyyMMddHHmmss形式の場合はミリ秒に000が設定される。
     * 
     * @param dateTimeExpression 固定日時
     */
    public void setFixedDate(String dateTimeExpression) {
        int len = dateTimeExpression.length();

        String dt = null;
        if (len == LONGEST_FORMAT.length()) {
            dt = dateTimeExpression;
        } else if (len == SHORTEST_FORMAT.length()) {
            // 右0詰めして、ミリ秒に000を設定
            dt = StringUtil.rpad(dateTimeExpression, LONGEST_FORMAT.length(), '0');
        } else {
            throw new IllegalArgumentException("datetime string " + dateTimeExpression);
        }
        doSetFixedDate(dt, LONGEST_FORMAT);
    }
    
    /**
     * 固定日時を設定する。
     * @param dateTimeExpression 日時表現
     * @param format 日時フォーマット
     */
    private void doSetFixedDate(String dateTimeExpression, String format) {
        try {
            fixedDate = new SimpleDateFormat(format).parse(dateTimeExpression);
        } catch (ParseException e) {
            throw new IllegalArgumentException("invalid datetime. expected=" + format + ", actual=" + dateTimeExpression, e);
        }
    }

    /**
     * 現在日時を取得する。
     * 
     * @return 現在日時
     */
    public Date getDate() {
        if (fixedDate == null) {
            throw new IllegalStateException("uninitialized.");
        }
        return (Date) fixedDate.clone();
    }

    /**
     * 現在日時を取得する。
     * 
     * @return 現在日時
     */
    public Timestamp getTimestamp() {
        return new Timestamp(getDate().getTime());
    }
}
