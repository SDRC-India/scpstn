/*
 * Copyright (C) 2011 University of Washington
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.sdrc.scpstn.collect.android.widgets;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.SelectOneData;
import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.form.api.FormEntryPrompt;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.sdrc.scpstn.collect.android.R;
import org.sdrc.scpstn.collect.android.application.Collect;
import org.sdrc.scpstn.collect.android.external.ExternalDataUtil;
import org.sdrc.scpstn.collect.android.external.ExternalSelectChoice;
import org.sdrc.scpstn.collect.android.utilities.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * ListWidget handles select-one fields using radio buttons. The radio buttons are aligned
 * horizontally. They are typically meant to be used in a field list, where multiple questions with
 * the same multiple choice answers can sit on top of each other and make a grid of buttons that is
 * easy to navigate quickly. Optionally, you can turn off the labels. This would be done if a label
 * widget was at the top of your field list to provide the labels. If audio or video are specified
 * in the select answers they are ignored.
 *
 * @author Jeff Beorse (jeff@beorse.net)
 */
public class ListWidget extends QuestionWidget implements OnCheckedChangeListener {

    List<SelectChoice> items; // may take a while to compute
    ArrayList<RadioButton> buttons;
    View center;
    private TextView label;
    private TextView missingImage;
    private ImageView imageView;

    public ListWidget(Context context, FormEntryPrompt prompt, boolean displayLabel) {
        super(context, prompt);

        // SurveyCTO-added support for dynamic select content (from .csv files)
        XPathFuncExpr xpathFuncExpr = ExternalDataUtil.getSearchXPathExpression(
                prompt.getAppearanceHint());
        if (xpathFuncExpr != null) {
            items = ExternalDataUtil.populateExternalChoices(prompt, xpathFuncExpr);
        } else {
            items = prompt.getSelectChoices();
        }
        buttons = new ArrayList<>();

        // Layout holds the horizontal list of buttons
        LinearLayout buttonLayout = new LinearLayout(context);

        String s = null;
        if (prompt.getAnswerValue() != null) {
            s = ((Selection) prompt.getAnswerValue().getValue()).getValue();
        }

        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                View answerLayout = inflate(context, R.layout.list_widget_item_layout, null);

                imageView = (ImageView) answerLayout.findViewById(R.id.image);
                missingImage = (TextView) answerLayout.findViewById(R.id.missingImage);
                label = (TextView) answerLayout.findViewById(R.id.label);
                RadioButton radioButton = (RadioButton) answerLayout.findViewById(R.id.radioButton);

                initRadioButton(radioButton, prompt, i);
                initImageView(prompt, i);
                initLabel(prompt, i);

                buttons.add(radioButton);

                if (items.get(i).getValue().equals(s)) {
                    radioButton.setChecked(true);
                }

                if (!displayLabel) {
                    label.setVisibility(GONE);
                    imageView.setVisibility(GONE);
                }

                // Each button gets equal weight
                LinearLayout.LayoutParams answerParams =
                        new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                                LayoutParams.MATCH_PARENT);
                answerParams.weight = 1;

                buttonLayout.addView(answerLayout, answerParams);
            }
        }
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        addAnswerView(buttonLayout);
    }

    private void initLabel(FormEntryPrompt prompt, int i) {
        // Don't assign the text to the built in label to he
        // button because it aligns horizontally, and we want the label on top
        label.setText(prompt.getSelectChoiceText(items.get(i)));
        label.setTextSize(TypedValue.COMPLEX_UNIT_DIP, questionFontsize);
    }

    private void initImageView(FormEntryPrompt prompt, int i) {

        String imageURI = getImageURI(prompt, i);

        // Now set up the image view
        String errorMsg = null;
        if (imageURI != null) {
            try {
                String imageFilename =
                        ReferenceManager._().DeriveReference(imageURI).getLocalURI();
                final File imageFile = new File(imageFilename);
                if (imageFile.exists()) {
                    Bitmap b = null;
                    try {
                        DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
                        int screenWidth = metrics.widthPixels;
                        int screenHeight = metrics.heightPixels;
                        b = FileUtils.getBitmapScaledToDisplay(imageFile, screenHeight,
                                screenWidth);
                    } catch (OutOfMemoryError e) {
                        errorMsg = "ERROR: " + e.getMessage();
                    }

                    if (b != null) {
                        imageView.setImageBitmap(b);
                        label.setVisibility(GONE);
                    } else if (errorMsg == null) {
                        // An error hasn't been logged and loading the image failed, so it's
                        // likely
                        // a bad file.
                        errorMsg = getContext().getString(R.string.file_invalid, imageFile);
                    }
                } else {
                    // An error hasn't been logged. We should have an image, but the file
                    // doesn't
                    // exist.
                    errorMsg = getContext().getString(R.string.file_missing, imageFile);
                }

                if (errorMsg != null) {
                    // errorMsg is only set when an error has occured
                    missingImage.setText(errorMsg);
                    missingImage.setVisibility(VISIBLE);
                    Timber.e(errorMsg);
                }
            } catch (InvalidReferenceException e) {
                Timber.e(e, "Invalid image reference due to %s ", e.getMessage());
            }
        } else {
            // There's no imageURI listed, so just ignore it.
        }

    }

    private String getImageURI(FormEntryPrompt prompt, int i) {
        if (items.get(i) instanceof ExternalSelectChoice) {
            return ((ExternalSelectChoice) items.get(i)).getImage();
        } else {
            return prompt.getSpecialFormSelectChoiceText(items.get(i),
                    FormEntryCaption.TEXT_FORM_IMAGE);
        }
    }

    private void initRadioButton(RadioButton radioButton, FormEntryPrompt prompt, int i) {
        radioButton.setTag(i);
        radioButton.setEnabled(!prompt.isReadOnly());
        radioButton.setFocusable(!prompt.isReadOnly());
        radioButton.setOnCheckedChangeListener(this);
    }

    @Override
    public void clearAnswer() {
        for (RadioButton button : this.buttons) {
            if (button.isChecked()) {
                button.setChecked(false);
                return;
            }
        }
    }

    @Override
    public IAnswerData getAnswer() {
        int i = getCheckedId();
        if (i == -1) {
            return null;
        } else {
            SelectChoice sc = items.get(i);
            return new SelectOneData(new Selection(sc));
        }
    }

    @Override
    public void setFocus(Context context) {
        // Hide the soft keyboard if it's showing.
        InputMethodManager inputManager =
                (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(this.getWindowToken(), 0);
    }


    public int getCheckedId() {
        for (int i = 0; i < buttons.size(); ++i) {
            RadioButton button = buttons.get(i);
            if (button.isChecked()) {
                return i;
            }
        }
        return -1;
    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (!isChecked) {
            // If it got unchecked, we don't care.
            return;
        }

        for (RadioButton button : this.buttons) {
            if (button.isChecked() && !(buttonView == button)) {
                button.setChecked(false);
            }
        }
        Collect.getInstance().getActivityLogger().logInstanceAction(this, "onCheckedChanged",
                items.get((Integer) buttonView.getTag()).getValue(), formEntryPrompt.getIndex());
    }


    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        for (RadioButton r : buttons) {
            r.setOnLongClickListener(l);
        }
    }


    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        for (RadioButton r : buttons) {
            r.cancelLongPress();
        }
    }

    @Override
    protected void addQuestionMediaLayout(View v) {
        center = new View(getContext());
        RelativeLayout.LayoutParams centerParams = new RelativeLayout.LayoutParams(0, 0);
        centerParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        center.setId(QuestionWidget.newUniqueId());
        addView(center, centerParams);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        params.addRule(RelativeLayout.LEFT_OF, center.getId());
        addView(v, params);
    }
}
