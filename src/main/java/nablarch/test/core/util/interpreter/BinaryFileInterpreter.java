package nablarch.test.core.util.interpreter;

import nablarch.core.util.BinaryUtil;
import nablarch.core.util.FileUtil;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static nablarch.core.util.Builder.concat;

/**
 * ファイルデータを解釈するクラス。<br/>
 * <p>
 * テストデータでファイルを表す場合に使用する。 テストデータの値として
 * 
 * <pre>
 * ${binaryFile:ファイルパス}
 * </pre>
 * 
 * と記述されていた場合、 そのファイルの内容をバイナリで読み込み HexString に変換する。<br/>
 * ファイルパスはExcelファイルからの相対パスで記述する。<br/>
 * <br/>
 * この Interpreter は自動テストフレームワークの内部で動的に追加されるため、<br/>
 * 設定ファイルの interpreters リスト要素に含める必要はない。
 * </p>
 * 
 * @author Shinsuke Yoshio
 */
public class BinaryFileInterpreter implements TestDataInterpreter {

    /** Excelの記述形式 */
    private static final Pattern PTN = Pattern.compile("\\$\\{binaryFile:(.+)\\}");

    /** ファイルの取得元パス */
    private String path;

    /**
     * コンストラクタ。
     * 
     * @param path ファイルの取得元パス
     */
    public BinaryFileInterpreter(String path) {
        this.path = path;
    }

    /** {@inheritDoc} */
    public String interpret(InterpretationContext context) {
        String value = context.getValue();
        Matcher m = PTN.matcher(value);
        return (m.matches()) ? fileToHexString(getPath(m.group(1))) : context.invokeNext();
    }

    /**
     * ファイルパスを取得する。
     * 
     * @param value Excelに記述された値（Excelファイルからの相対パス）
     * @return テストデータのファイルパス
     */
    private String getPath(String value) {
        return concat(path, '/', value);
    }

    /**
     * ファイルの内容を HexString に変換する。
     * 
     * @param path ファイルパス
     * @return ファイル内容を表す HexString
     */
    private String fileToHexString(String path) {
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            fis = new FileInputStream(path);
            bis = new BufferedInputStream(fis);
            int ch;
            while ((ch = bis.read()) != -1) {
                baos.write(ch);
            }
        } catch (IOException e) {
            throw new RuntimeException("an error occurred while reading file:" + path, e);
        } finally {
            FileUtil.closeQuietly(fis, bis);
        }

        return BinaryUtil.convertToHexString(baos.toByteArray());
    }
}
