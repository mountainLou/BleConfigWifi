package idea.analyzesystem.android.edittext.mac;

import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import idea.analyzesystem.android.edittext.AbsEditText;
import idea.analyzesystem.android.edittext.AbsEditTextGroup;

import static android.content.Context.CLIPBOARD_SERVICE;

/**
 * Created by idea on 2016/7/15.
 */
public class MacView extends AbsEditTextGroup {
    private boolean isClickPaste = false;
    private ClipboardManager mClipboardManager;

    public MacView(Context context) {
        this(context,null,0);
    }

    public MacView(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public MacView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (isClickPaste) {
            isClickPaste = false;
            mClipboardManager = (ClipboardManager) getContext().getSystemService(CLIPBOARD_SERVICE);
            if (mClipboardManager.hasPrimaryClip()
                    && mClipboardManager.getPrimaryClip().getItemCount() > 0) {
                CharSequence content =
                        mClipboardManager.getPrimaryClip().getItemAt(0).getText();
                String[] subMacString = new String[6];
                if(macString2List(content.toString(),subMacString)){
                    for (int i=0; i< editTexts.size(); i++){
                        if(editTexts.get(i).hasFocus()){ //hasFocus √ & isFocus ×
                            editTexts.get(i).clearFocus();
                        }
                        editTexts.get(i).setText(subMacString[i]);
                    }
                    editTexts.get(editTexts.size()-1).requestFocus();
                    editTexts.get(editTexts.size()-1).setSelection(getDelMaxLength());
                }
            }
        }
        else{
            super.afterTextChanged(s);
        }
    }

    public boolean macString2List(String macString, String[] subMacStrings){
        boolean ret = false;
        if (subMacStrings.length != 6){
            ret = false;
            return ret;
        }
        String[] macStrings = macString.split(":|\\-");
        if (macStrings.length != 6){
            ret = false;
            return ret;
        }

        for (int i = 0; i< 6;i++){
            subMacStrings[i] = macStrings[i];
        }

        ret = true;
        return ret;
    }

    @Override
    public int getChildCount() {
        return 11;
    }

    @Override
    public AbsEditText getAbsEditText() {
        return new MacEditText(getContext());
    }

    @Override
    protected void addViews() {
        for (int i = 0; i < getChildCount(); i++) {
            if (i%2==0) {
                if(i==0){
                    AbsEditText absEditText = new MacEditText(getContext());
                    ((MacEditText) absEditText).setOnPasteCallback(new MacEditText.OnPasteCallback(){
                        @Override
                        public void onPaste() {
                            //检测粘贴的数据
                            isClickPaste = true;
                        }
                    });
                    LayoutParams params = new LayoutParams(0, LayoutParams.MATCH_PARENT);
                    params.weight = 1;
                    absEditText.setLayoutParams(params);
                    absEditText.setTextSize(sp16);//sp
                    absEditText.setTextColor(0xFF888888);
                    absEditText.setGravity(Gravity.CENTER);
                    absEditText.setPadding(dp4, dp4, dp4, dp4);
                    absEditText.setSingleLine();
                    absEditText.setFocusableInTouchMode(true);
                    absEditText.setBackgroundDrawable(new ColorDrawable(0xFFFFFFFF));
                    applyEditTextTheme(absEditText);

                    editTexts.add(absEditText);
                    addView(absEditText);
                }else{
                    AbsEditText absEditText= createAbsEditText();
                    editTexts.add(absEditText);
                    addView(absEditText);
                }
            } else {
                addView(createSemicolonTextView());
            }
        }
    }
    

    @Override
    public String getSemicolomText() {
        return ":";
    }

    @Override
    public int getDelMaxLength() {
        return 2;
    }

    @Override
    public void applySemicolonTextViewTheme(TextView semicolonTextView) {
        semicolonTextView.setGravity(Gravity.CENTER);
        semicolonTextView.setPadding(dp4, dp4, dp4, dp4);
    }

    @Override
    public void applyEditTextTheme(AbsEditText absEditText) {

    }

    @Override
    public boolean checkInputValue() {
        return super.checkInputValue();
    }

    public void setMacText(String[] macValues){
        for(int i=0; i<editTexts.size(); i++){
            editTexts.get(i).setText(macValues[i]);
        }
    }

    public String getMacAddress(){
        String result = "";
        for(int i=0; i<editTexts.size()-1; i++){
            result+= editTexts.get(i).getText().toString().trim()+":";
        }
        result+= editTexts.get(5).getText().toString().trim();

        return result;
    }

    /**
     * setAllFocuse false 禁用编辑器
     * @param focuse
     */
    public void setAllFocuse(boolean focuse){
        for(int i=0; i<editTexts.size(); i++){
            editTexts.get(i).setFocusable(focuse);
            editTexts.get(i).setFocusableInTouchMode(focuse);
        }
    }

    public void setChildIndexText(int index,String text){
        editTexts.get(index).setText(text);
    }
}
