package nablarch.test.core.util.interpreter;

/**
 * 引用符を取り除く解釈クラス。<br/>
 * 要素の値が、ダブルクォート(半角、全角問わず)で囲われている場合は、前後のダブルクォートを削除する。
 *
 * @author T.Kawasaki
 */
public class QuotationTrimmer implements TestDataInterpreter {

    /** {@inheritDoc} */
    public String interpret(InterpretationContext context) {
        String trimmed = trimQuotation(context.getValue());
        context.setValue(trimmed);
        return context.invokeNext();
    }

    /**
     * 対象文字列から引用符を取り除く。
     *
     * @param str 対象文字列
     * @return 引用符を取り除いた文字列
     */
    private String trimQuotation(String str) {
        // 長さ1（引用符が1文字だけ）は前後を囲っているとは見なせない。
        // 長さ1を囲みと誤判定すると substring(1, 0) で例外となるため除外する。
        if (str.length() < 2) {
            return str;
        }
        if ((str.startsWith("\"") && str.endsWith("\""))
                || (str.startsWith("”") && str.endsWith("”"))) {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }

}
