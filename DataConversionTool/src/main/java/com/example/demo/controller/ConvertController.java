package com.example.demo.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.example.demo.constant.Constants;
import com.example.demo.constant.Settings;
import com.example.demo.exception.ApplicationException;
import com.example.demo.form.ConvertForm;
import com.example.demo.form.ConvertWpForm;
import com.example.demo.service.AnalyzeService;
import com.example.demo.util.ConvertUtil;

/**
 * データコンバート　コントローラ
 */

@Controller
@RequestMapping("/")
public class ConvertController {
	@Autowired
    MessageSource messagesource;

	@Autowired
	private Settings settings;

	@Autowired
	private ConvertUtil convertUtil;

	@Autowired
    protected ResourceLoader resourceLoader;

	@Autowired
	private AnalyzeService analyzeService;

	/**
	 * データ変換処理画面表示
	 *
	 * @param convertWpForm
	 * @return
	 * @throws Exception
	 */
	@GetMapping(value = "contents")
	public ModelAndView index(ConvertWpForm convertWpForm) throws Exception {
		ModelAndView mav = new ModelAndView();
        mav.setViewName("index/contents");

		// 移行先クラス定義の一覧を取得する。
		List<String> classNames = convertUtil.getClassNameList(1, settings.getOutPath());
		Collections.sort(classNames);
		// 移行設定ファイルの読み込み
		Properties convProps = new Properties();
		String strPath = settings.getConvertProperties();
		try {
			File file = new File(strPath);
			if (file.exists()) {
				InputStream istream = new FileInputStream(strPath);
				convProps.load(istream);
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
			throw new ApplicationException("予期せぬエラーが発生しました。", e);
		}

		String className = null;
		String memberName = null;
		String typeName = null;
		String inDataName = null;
		String outDataName = null;
		List<ConvertForm> convList = new ArrayList<>();
		ConvertForm form;
		// 移行先クラス名一覧をループ
		for(int i = 0; i < classNames.size(); ++i){
			// テーブル名設定
			className = classNames.get(i);
			form = new ConvertForm();
			form.setTableName(className);
			// プロパティファイルに設定済みの場合取得する
			inDataName = convProps.getProperty(className);
			if (!StringUtils.isEmpty(inDataName)) {
				form.setSettingValue(inDataName);
			}
			convList.add(form);

			// カラム名,型設定
			// 移行先クラスメンバーの取得
			List<String> propKeys = convertUtil.getClassMemberNameList(2, settings.getOutPath() + Constants.Package.SEPARATOR + className);
			for (int x = 0; x < propKeys.size(); ++x) {
				memberName = propKeys.get(x).split(Constants.Character.COMMA)[0];
				typeName = propKeys.get(x).split(Constants.Character.COMMA)[1];

				form = new ConvertForm();
				form.setColumnName(memberName);
				form.setTypeName(typeName);
				// プロパティファイルに設定済みの場合取得する
				outDataName = convProps.getProperty(className + Constants.Package.SEPARATOR + memberName);
				String value;
				try {
					value = outDataName.substring(outDataName.indexOf(Constants.Character.COMMA) + 1);
				} catch (Exception e) {
					value = "";
				}
				if (!StringUtils.isEmpty(value)) {
					form.setSettingValue(value);
				}
				convList.add(form);
			}
		}

		convertWpForm.setConvList(convList);
		if (Objects.equals(convProps.getProperty("afCheckBox"),"on")) {
			convertWpForm.setAfCheckBox("on");
		}
        mav.addObject("convertWpFormData", convertWpForm);

		return mav;
	}


	/**
	 * データ変換処理実行
	 *
	 * @param model
	 * @param convertWpForm
	 * @param bindingResult
	 * @param request
	 * @return
	 */
	@PostMapping("submit")
	public String messagesPost(Model model, @Valid ConvertWpForm convertWpForm, BindingResult bindingResult,	HttpServletRequest request) {
		//TODO エラーチェック
		if (bindingResult.hasErrors()) {
//            List<Message> messages = service.getRecentMessages(100);
//            model.addAttribute("messages", messages);
			return "messages";
		}

		// 移行設定ファイルへ書き込み
		Properties properties = new Properties();
		try {
			String tableName = null;
			String columnName ,key ,value;
            for (int i = 0; i < convertWpForm.getConvList().size(); i++) {
				key = convertWpForm.getConvList().get(i).getTableName();
				columnName = convertWpForm.getConvList().get(i).getColumnName();
				value = convertWpForm.getConvList().get(i).getSettingValue();
				if (StringUtils.isEmpty(columnName)) {
					tableName = convertWpForm.getConvList().get(i).getTableName();
				} else {
					// カラム項目の場合
					value = StringUtils.isEmpty(value) ? "0," + value : "1," + value;
					key = tableName + Constants.Character.PERIOD + columnName;
 				}
				properties.setProperty(key, value);
			}
            // チェックボックス
    		if (Objects.equals(convertWpForm.getAfCheckBox(),"on")) {
    			properties.setProperty("afCheckBox", convertWpForm.getAfCheckBox());
    		}

			OutputStream ostream = new FileOutputStream(settings.getConvertProperties());
			OutputStreamWriter osw = new OutputStreamWriter(ostream, "UTF-8");
			properties.store(osw, "Comments");
			ostream.close();
		} catch (Exception e) {
			System.err.println(e.getMessage());
			throw new ApplicationException("予期せぬエラーが発生しました。", e);
		}

		// 移行元データインスタンスを生成（一旦EXCELファイルよりデータ取得）
		List<Object> inDataInstance = null;
		try {
			Resource resource = resourceLoader.getResource("classpath:" + settings.getInDataPath());
			File res = resource.getFile();

			inDataInstance = convertUtil.generateInstance(res, settings.getInPath());
			if (inDataInstance == null) {
				String errMsg = "データが見つかりません。";
				throw new Exception(errMsg);
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
			throw new ApplicationException("予期せぬエラーが発生しました。", e);
		}

		// ユーザファンクション一覧読み込み
		List<String> functionList = new ArrayList<>();
		functionList = convertUtil.getClassMethodNameList(settings.getUserFunction());

		// 設定ファイル読み込み
		// 移行先クラス定義の一覧を取得する。
		List<String> classNames = convertUtil.getClassNameList(1, settings.getOutPath());
		Collections.sort(classNames);
		// 移行設定ファイルの読み込み
		Properties convProps = new Properties();
		String strPath = settings.getConvertProperties();
		try {
			InputStream istream = new FileInputStream(strPath);
			convProps.load(istream);
		} catch (IOException e) {
			System.err.println(e.getMessage());
			throw new ApplicationException("予期せぬエラーが発生しました。", e);
		}

		String className = null;
		String memberName = null;
		String inDataName = null;
		List<Object> invokeObjectList = new ArrayList<Object>();
		// 移行先クラス名一覧をループ
		for(int i = 0; i < classNames.size(); ++i){
			className = classNames.get(i);
			// プロパティファイルから基本移行元クラス名の取得
			inDataName = convProps.getProperty(className);
			if (StringUtils.isEmpty(inDataName)) {
				continue;
			}
			// 移行先クラスメンバーの取得
			List<String> propKeys = convertUtil.getClassMemberNameList(0, settings.getOutPath() + Constants.Package.SEPARATOR + className);

			// 移行元対象クラスデータを取得
			Class<?> cls = convertUtil.getClassForName(settings.getInPath() + Constants.Package.SEPARATOR + inDataName);
			List<Object> inDataList = inDataInstance.stream().filter(cls::isInstance).collect(Collectors.toList());

			// 移行元クラスデータをループ
			for (int idx = 0; idx < inDataList.size(); ++idx) {
				Object inData = inDataList.get(idx);

				// クラスオブジェクト生成
				Object obj = null;
				try {
					obj = convertUtil.newInstance(settings.getOutPath() + Constants.Package.SEPARATOR + className);
				} catch (Exception e) {
					System.err.println(e.getMessage());
					throw new ApplicationException("予期せぬエラーが発生しました。", e);
				}

				// 移行先クラスのオブジェクトをループ
				for (String propKey : propKeys) {
					// 移行先メンバー名取得
					memberName = propKey.substring(propKey.lastIndexOf(Constants.Package.SEPARATOR) + 1);

					// データ移行情報取得
					Object editingInfo = analyzeService.getAnalyzeInfo(convProps.getProperty(propKey), inData, functionList);
					if (editingInfo == null) {
						continue;
					}
					// 移行先にデータセット
					try {
						convertUtil.setProperty(obj, memberName, editingInfo);
					} catch (Exception e) {
						System.err.println(e.getMessage());
						throw new ApplicationException("予期せぬエラーが発生しました。", e);
					}
				}

				invokeObjectList.add(obj);
	    	}
		}

		// アフターファンクション実行
		if (Objects.equals(convertWpForm.getAfCheckBox(),"on")) {
			analyzeService.excecAfterFunction(inDataInstance, invokeObjectList);
		}

		return "redirect:/contents";
	}

}
