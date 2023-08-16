package nablarch.test.core.entity;

import nablarch.core.repository.SystemRepository;
import nablarch.test.core.util.generator.CharacterGenerator;

import static nablarch.core.util.Builder.concat;

/**
 * エンティティテスト用設定クラス。
 *
 * @author T.Kawasaki
 */
public class EntityTestConfiguration {

    /** 最長桁超過時のメッセージID */
    private String maxMessageId;

    /**
     * 最長最短桁範囲外のメッセージID(max > min)。
     * 最大桁数を超過した場合。
     */
    private String maxAndMinMessageId;

    /** 最長最短桁範囲外のメッセージID(max == min) */
    private String fixLengthMessageId;

    /**
     * 最長最短桁範囲外のメッセージID(max > min)。
     * 最小桁数より不足した場合。
     */
    private String underLimitMessageId;

    /** 桁数不足時のメッセージID */
    private String minMessageId;

    /** 未入力時のメッセージID */
    private String emptyInputMessageId;

    /** 文字列生成クラス */
    private CharacterGenerator characterGenerator;

    /** バリデーションストラテジ */
    private ValidationTestStrategy validationTestStrategy = new NablarchValidationTestStrategy();

    /** テスト設定取得用のキー */
    private static final String CONFIG_KEY = "entityTestConfiguration";

    /**
     * テスト設定を取得する。
     *
     * @return テスト設定
     */
    public static EntityTestConfiguration getConfig() {
        EntityTestConfiguration config = SystemRepository.get(CONFIG_KEY);
        if (config == null) {
            throw new IllegalStateException("can't get EntityTestConfiguration from SystemRepository."
                    + " key=[" + CONFIG_KEY + "]");
        }
        return config;
    }

    /**
     * 未入力時のメッセージIDを取得する。
     *
     * @return 未入力時のメッセージID
     */
    public String getEmptyInputMessageId() {
        return emptyInputMessageId;
    }

    /**
     * 桁数不足テスト時に期待するメッセージIDを取得する。
     * 許容する桁数に応じて適切なメッセージIDを返却される。
     *
     *
     * @param max 最長桁数
     * @param min 最短桁数
     * @return 桁数超過時のメッセージID
     */
    String getUnderLimitMessageId(Integer max, Integer min) {
        // 最大桁数及び桁数不足メッセージの両方が設定されていない場合はエラーとする。
        if (null == max && null == minMessageId) {
            throw new IllegalArgumentException("If max is not specified, minMessageId must be specified.");
        }

        if (null == max) {
            return minMessageId;      // 最短のみ
        } else if (max.equals(min)) {
            return fixLengthMessageId;
        } else if (max > min) {
            return underLimitMessageId;
        }
        throw new IllegalArgumentException(concat(
                "invalid argument. max=[", max, "]", "min=[", min, "]"));
    }

    /**
     * 桁数超過時のメッセージIDを取得する。
     * 許容する桁数に応じて適切なメッセージIDを返却される。
     *
     * @param max 最大桁数
     * @param min 最小桁数
     * @return 桁数不足時のメッセージID
     */
    String getOverLimitMessageId(Integer max, Integer min) {

        if (min == null) {
            return maxMessageId;          // 最長のみ
        } else if (max.equals(min)) {
            return fixLengthMessageId;    // 固定長
        } else if (max > min) {
            return maxAndMinMessageId;    // 最長最短
        }
        throw new IllegalArgumentException(concat(
                "invalid argument. max=[", max, "]", "min=[", min, "]"));
    }

    /**
     * 未入力時のメッセージIDを設定する。
     *
     * @param emptyInputMessageId 未入力時のメッセージID
     */
    public void setEmptyInputMessageId(String emptyInputMessageId) {
        this.emptyInputMessageId = emptyInputMessageId;
    }

    /**
     * 桁数超過時のメッセージIDを設定する（最大桁のみ）。
     *
     * @param maxMessageId 桁数超過時のメッセージID
     */
    public void setMaxMessageId(String maxMessageId) {
        this.maxMessageId = maxMessageId;
    }

    /**
     * 桁数不足時のメッセージIDを設定する。
     *
     * @param underLimitMessageId 桁数不足時のメッセージID
     */
    public void setUnderLimitMessageId(String underLimitMessageId) {
        this.underLimitMessageId = underLimitMessageId;
    }

    /**
     * 桁数の最大値が指定されない場合の、桁数不足時のメッセージIDを設定する。
     *
     * @param minMessageId 桁数不足時のメッセージID
     */
    public void setMinMessageId(String minMessageId) {
        this.minMessageId = minMessageId;
    }

    /**
     * 桁数誤り時のメッセージIDを設定する（最大桁＝最小桁）。
     *
     * @param fixLengthMessageId 桁数誤り時のメッセージID
     */
    public void setFixLengthMessageId(String fixLengthMessageId) {
        this.fixLengthMessageId = fixLengthMessageId;
    }

    /**
     * 桁数超過時のメッセージIDを設定する（最大桁＞最小桁）。
     *
     * @param maxAndMinMessageId 桁数超過時のメッセージID
     */
    public void setMaxAndMinMessageId(String maxAndMinMessageId) {
        this.maxAndMinMessageId = maxAndMinMessageId;
    }

    /**
     * 文字列生成クラスを取得する。
     *
     * @return 文字列生成クラス
     */
    public CharacterGenerator getCharacterGenerator() {
        return characterGenerator;
    }

    /**
     * 文字列生成クラスを設定する。
     *
     * @param characterGenerator 文字列生成クラス
     */
    public void setCharacterGenerator(CharacterGenerator characterGenerator) {
        this.characterGenerator = characterGenerator;
    }

    /**
     * テスト用バリデーションストラテジを取得する。
     *
     * @return テスト用バリデーションストラテジ
     */
    public ValidationTestStrategy getValidationTestStrategy(){
        return validationTestStrategy;
    }

    /**
     * テスト用バリデーションストラテジを設定する。
     *
     * @param validationTestStrategy テスト用バリデーションストラテジ
     */
    public void setValidationTestStrategy(ValidationTestStrategy validationTestStrategy) {
        this.validationTestStrategy = validationTestStrategy;
    }
}
