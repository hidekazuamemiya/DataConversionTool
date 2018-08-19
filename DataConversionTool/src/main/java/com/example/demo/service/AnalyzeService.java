package com.example.demo.service;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.constant.Constants;
import com.example.demo.constant.Settings;
import com.example.demo.dto.Token;
import com.example.demo.exception.ApplicationException;
import com.example.demo.util.ConvertUtil;
import com.example.demo.util.InterpreterUtil;
import com.example.demo.util.LexerUtil;
import com.example.demo.util.ParserUtil;

/**
 * データ解析　サービス
 */

@Service
public class AnalyzeService {

	private Settings settings;
	@Autowired
    public void Settings(Settings settings) {
        this.settings = settings;
    }

	@Autowired
	private ConvertUtil convertUtil;

    /**
     * 解析情報の返却
     *
     * @param info
     * @return
     */
	public Object getAnalyzeInfo(String info, Object inData, List<String> functionList) {
		int key = Integer.parseInt(info.substring(0, info.indexOf(Constants.Character.COMMA)));
		String value = info.substring(info.indexOf(Constants.Character.COMMA) + 1);
		switch (key) {
		// 0: 何もしない
		// 1: 設定値を使用する
		case 1:
			List<Token> tokens = null;
			List<Token> blk = null;
			try {
				// 字句解析
				tokens = new LexerUtil().init(value, functionList).tokenize();
				// 変数、属性の変換
				for (int i = 0; i < tokens.size(); i++) {
					if (tokens.get(i).getKind().equals("ident")) {
						String str = getObjectData(tokens.get(i).getValue(), inData);
						tokens.get(i).setKind(kindChange(str));
						tokens.get(i).setValue(str);
					} else 	if (tokens.get(i).getKind().equals("function")) {
						try {
							Object invokeObject = convertUtil.newInstance(settings.getUserFunction());
							String callMethod = tokens.get(i).value.toString().substring(0, tokens.get(i).value.toString().indexOf("("));
							String arg = tokens.get(i).value.toString().substring(tokens.get(i).value.toString().indexOf("(") + 1, tokens.get(i).value.toString().indexOf(")"));
							String[] args = null;
							if (!StringUtils.isEmpty(arg)) {
								args = arg.split(Constants.Character.COMMA,-1);
								// パラメータ解析
								args = paramParser(args, inData);
							}
							// メソッド実行
							String str = convertUtil.invoke(invokeObject, callMethod, args).toString();
							tokens.get(i).setKind(kindChange(str));
							tokens.get(i).setValue(str);
						} catch (Exception e) {
							System.err.println(e.getMessage());
							throw new ApplicationException("予期せぬエラーが発生しました。", e);
						}
					}
				}
				// 構文解析
				blk = new ParserUtil().init(tokens).block();
				Object obj = new InterpreterUtil().init(blk).run();
				return obj;
			} catch (Exception e) {
				System.err.println(e.getMessage());
				throw new ApplicationException("予期せぬエラーが発生しました。", e);
			}

		default:
			return null;
		}
	}

	/**
	 * 対象データの返却
	 *
	 * @param value
	 * @return
	 */
	public String getObjectData(String value, Object inData) {
		String[] str = value.split("\\.");
		String mName = null;
		try {
			// メンバー名
			mName = str[1];
		} catch (Exception e) {
			// xxx.xxxの形式でなく文字が入力されている場合
			return str[0];
		}
		// 対象クラスのメソッド取得
		// GETメソッド実行
		try {
			return convertUtil.getProperty(inData, mName).toString();
		} catch (Exception e) {
			System.err.println(e.getMessage());
			throw new ApplicationException("予期せぬエラーが発生しました。", e);
		}

	}

	/**
	 * パラメータ解析
	 *
	 * @param params
	 * @return boolean
	 */
	public String[] paramParser(String[] params, Object inData) {
		String[] args = new String[params.length];
		for (int i = 0; i < params.length; i++) {
			String str = StringUtils.strip(params[i], "\"");
			if (str.indexOf(".") > -1) {
				str = getObjectData(str, inData);
			}
			args[i] = str;
		}
		return args;
	}

	/**
	 * 属性の返却
	 *
	 * @param str
	 * @return
	 */
	public String kindChange(String str) {
		if (LexerUtil.isCharacter(str)) {
			return "string";
		} else {
			// 「01」を文字と判定する
			if (str.equals(String.valueOf(Integer.parseInt(str)))) {
				return "digit";
			} else {
				return "string";
			}
		}
	}

    /**
     * アフターファンクション実行
     *
     * @param inData
     * @param outData
     */
	public void excecAfterFunction(List<Object> inData, List<Object> outData) {
		try {
			Object invokeObject = convertUtil.newInstance(settings.getAfterFunctionService());
			String callMethod = settings.getAfterFunction();
			Object[] args = new Object[2];
			args[0] = inData;
			args[1] = outData;
			// メソッド実行
			convertUtil.invoke(invokeObject, callMethod, args);
		} catch (Exception e) {
			System.err.println(e.getMessage());
			throw new ApplicationException("予期せぬエラーが発生しました。", e);
		}
	}

}
