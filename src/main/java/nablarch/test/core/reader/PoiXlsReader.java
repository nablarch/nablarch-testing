package nablarch.test.core.reader;

import java.io.File;
import java.io.FileFilter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import nablarch.core.util.FileUtil;
import nablarch.core.util.StringUtil;
import nablarch.test.NablarchTestUtils;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/**
 * POIを使用してEXCELからテストデータを読み込むクラス。<br/>
 * EXCELに記述されたテストデータは、すべて文字列書式となっている必要がある。
 * 文字列書式以外のデータ書式が存在した場合の動作は保証しない。
 *
 * @author hisaaki sioiri 2009/12/15
 */
public class PoiXlsReader implements TestDataReader {

    /** データ名の区切り文字 */
    private static final Pattern DATA_NAME_SPLIT_CHAR = Pattern.compile("/");

    /** Excelシート */
    private Sheet sheet = null;

    /** シートの最終行 */
    private int lastRowNumber = 0;

    /** カレント行を示すインデックス */
    private int rowIdx = 0;

    /** キャッシュ要否。デフォルトはtrue */
    private boolean useCache = true;

    /** {@inheritDoc} */
    public void open(String path, String dataName) {
        if (StringUtil.isNullOrEmpty(dataName)) {
            throw new IllegalArgumentException("dataName must not be null or empty.");
        }

        // テストデータ名は、ファイル名/シート名
        // 末尾/も許容
        String[] split = DATA_NAME_SPLIT_CHAR.split(dataName);
        if (split.length != 2) {
            throw new IllegalArgumentException("invalid data name. [" + dataName + "]");
        }
        String fileName = split[0];
        String sheetName = split[1];

        File file = new File(path + '/' + fileName + ".xls");
        if (!file.exists()) {
            file = new File(path + '/' + fileName + ".xlsx");
        }
        String filePath = file.getAbsolutePath();
        Workbook book;
        if (useCache) {
            book = getCachedWorkbook(filePath); // キャッシュから取得
        } else {
            book = getWorkbook(filePath);       // キャッシュを使用せず取得
        }
        sheet = book.getSheet(sheetName);
        if (sheet == null) {
            String msg = "sheet not found. path=[" + filePath + "] sheet=[" + sheetName + "]";
            throw new IllegalArgumentException(msg);
        }
        lastRowNumber = sheet.getLastRowNum();
        rowIdx = 0;
    }

    /** {@inheritDoc} */
    public List<String> readLine() {
        while (true) {
            List<String> list = readOneLine();

            // 最終行に達した場合は、nullを返却
            if (list == null) {
                return null;
            }

            // 空行チェック
            if (isBlankLine(list)) {
                continue;
            }
            return list;
        }
    }

    /**
     * 1行分読み込みを行う
     *
     * @return 1行分のデータ
     */
    private List<String> readOneLine() {
        Row row = null;

        while (rowIdx <= lastRowNumber) {
            row = sheet.getRow(rowIdx++);
            if (row != null) {
                break;
            }
        }

        if (row == null) {
            return null;
        }

        List<String> line = new ArrayList<String>(64);
        int lastCellNum = row.getLastCellNum();
        for (int i = 0; i < lastCellNum; i++) {
            Cell cell = row.getCell(i);
            String cellValue = cell == null ? "" : cell.toString();
            line.add(cellValue);
            if (i == 0 && cellValue.startsWith("//")) {
                // 先頭カラムがコメントの場合は、これ以上値を読み込む必要はない
                break;
            }
        }
        return line;
    }


    /**
     * 全要素が空かどうか判定
     *
     * @param line １行分のデータ
     * @return 配列の全要素が空であればtrue、空でなければfalse
     */
    private boolean isBlankLine(List<String> line) {
        for (String e : line) {
            if (!e.isEmpty()) {
                return false;
            }
        }
        return true;
    }


    /** {@inheritDoc} */
    public void close() {
        sheet = null;
    }

    /** ブックのキャッシュサイズ */
    private static final int BOOK_CACHE_SIZE = 1;

    /** ブックのキャッシュ */
    private static Map<String, Workbook> bookCache = NablarchTestUtils.createLRUMap(BOOK_CACHE_SIZE);

    /**
     * キャッシュされたブックを取得する。<br/>
     * キャッシュにヒットしない場合は、ファイルから読み込みを行う。
     *
     * @param filePath ファイルパス
     * @return ブック
     */
    private static Workbook getCachedWorkbook(String filePath) {
        Workbook book = bookCache.get(filePath);
        if (book == null) {
            book = getWorkbook(filePath);
            bookCache.put(filePath, book);
        }
        return book;
    }

    /**
     * ブックを取得する。
     *
     * @param filePath Workbookを表すFile
     * @return ブック
     */
    private static Workbook getWorkbook(String filePath) {
        Workbook book;
        InputStream in = null;
        try {
            String uri = new File(filePath).toURI().toString();
            in = FileUtil.getResource(uri);
            book = WorkbookFactory.create(in);
        } catch (Exception e) {
            throw new RuntimeException("test data file open failed.", e);
        } finally {
            FileUtil.closeQuietly(in);
        }
        return book;
    }

    /**
     * シート名を取得する。
     *
     * @param file シート名を取得したいファイル
     * @return シート名
     */
    public static Set<String> getSheetNames(File file) {
        Workbook book = getCachedWorkbook(file.getPath());
        int size = book.getNumberOfSheets();
        Set<String> names = new HashSet<String>(size);
        for (int i = 0; i < size; i++) {
            names.add(book.getSheetName(i));
        }
        return names;
    }

    /**
     * キャッシュ要否を設定する。
     *
     * @param useCache キャッシュ要否
     */
    public void setUseCache(boolean useCache) {
        this.useCache = useCache;
    }

    /** 前回問い合わせのあったリソース名 */
    private static String prevResourceName;

    /**
     * 指定されたパスとリソース名に該当するExcelファイルが存在するか判定する。
     * @param basePath パス
     * @param resourceName リソース名
     * @return 存在する場合、真
     */
    public boolean isResourceExisting(String basePath, String resourceName) {
        final String[] splitted = splitLastResourceName(resourceName);
        String key = basePath + '/' + splitted[0];
        if (key.equals(prevResourceName)) {
            return true;
        }
        File dir = new File(basePath);
        File[] files1 = dir.listFiles(new FileFilter() {
            
            @Override
            public boolean accept(File pathname) {
                return isAcceptableFilename(splitted[0], pathname);
            }
        });
        if (files1 != null && files1.length > 0) {
            prevResourceName = key;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean isDataExisting(String basePath, String resourceName) {
        String[] split = DATA_NAME_SPLIT_CHAR.split(resourceName);
        if (split.length != 2) {
            throw new IllegalArgumentException("invalid data name. [" + resourceName + "]");
        }
        String fileName = split[0];
        String sheetName = split[1];

        File dir = new File(basePath);
        File file = new File(dir, fileName + ".xls");
        if (!file.exists()) {
            file = new File(dir, fileName + ".xlsx");
        }
        if (!file.exists()) {
            return false;
        }

        Set<String> sheetNames = PoiXlsReader.getSheetNames(file);
        return sheetNames.contains(sheetName);
    }

    /**
     * ファイル名が許容可能かチェックする。
     * 
     * @param resourcePrefix リソースのプレフィクス
     * @param file チェック対象のファイル
     * @return ファイル名が許容可能であればtrue
     */
    private boolean isAcceptableFilename(String resourcePrefix,
            File file) {
        String name = file.getName();
        int dotPosition = name.lastIndexOf('.');
        
        // dotPosition == name.length() になる「.で終わるファイル」のテストは Windows でテストできないので割愛。
        if (dotPosition < 0 || dotPosition == name.length()) {
            // 拡張子のないファイルは許容しない
            return false;
        }
        String extension = name.substring(name.lastIndexOf('.') + 1);
        return name.startsWith(resourcePrefix) && (extension.equals("xls") || extension.equals("xlsx"));
    }


    /**
     * ファイルパスを取得する。
     *
     * @param resourceName リソース名
     * @return ファイルパス
     */
    private String[] splitLastResourceName(String resourceName) {
        String name = resourceName;
        int index = resourceName.lastIndexOf('/');
        if (index == resourceName.length()) {
            name = resourceName.substring(0, resourceName.length() - 1);
        }
        String first = name.substring(0, index);
        String second = name.substring(index + 1);
        return new String[] {first, second};
    }

}
