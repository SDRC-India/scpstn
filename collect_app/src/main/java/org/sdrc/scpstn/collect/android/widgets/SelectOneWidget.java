/*
 * Copyright (C) 2009 University of Washington
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
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.SelectOneData;
import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.form.api.FormEntryPrompt;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.sdrc.scpstn.collect.android.application.Collect;
import org.sdrc.scpstn.collect.android.external.ExternalDataUtil;
import org.sdrc.scpstn.collect.android.external.ExternalSelectChoice;
import org.sdrc.scpstn.collect.android.listeners.AudioPlayListener;
import org.sdrc.scpstn.collect.android.utilities.TextUtils;
import org.sdrc.scpstn.collect.android.views.MediaLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * SelectOneWidgets handles select-one fields using radio buttons.
 *
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */
public class SelectOneWidget extends QuestionWidget implements
        OnCheckedChangeListener, AudioPlayListener {

    List<SelectChoice> items; // may take a while to compute
    ArrayList<RadioButton> buttons;
    ArrayList<MediaLayout> playList;
    private int playcounter = 0;


    public SelectOneWidget(Context context, FormEntryPrompt prompt) {
        super(context, prompt);
        playList = new ArrayList<MediaLayout>();

        // SurveyCTO-added support for dynamic select content (from .csv files)
        XPathFuncExpr xpathFuncExpr = ExternalDataUtil.getSearchXPathExpression(
                prompt.getAppearanceHint());
        if (xpathFuncExpr != null) {
            items = ExternalDataUtil.populateExternalChoices(prompt, xpathFuncExpr);
        } else {
            items = prompt.getSelectChoices();
        }
        buttons = new ArrayList<RadioButton>();

        // Layout holds the vertical list of buttons
        LinearLayout buttonLayout = new LinearLayout(context);

        String s = null;
        if (prompt.getAnswerValue() != null) {
            s = ((Selection) prompt.getAnswerValue().getValue()).getValue();
        }

        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                String choiceName = prompt.getSelectChoiceText(items.get(i));
                CharSequence choiceDisplayName;
                if (choiceName != null) {
                    choiceDisplayName = TextUtils.textToHtml(choiceName);
                } else {
                    choiceDisplayName = "";
                }
                RadioButton r = new RadioButton(getContext());
                r.setTextSize(TypedValue.COMPLEX_UNIT_DIP, answerFontsize);
                r.setText(choiceDisplayName);
                r.setMovementMethod(LinkMovementMethod.getInstance());
                r.setTag(Integer.valueOf(i));
                r.setId(QuestionWidget.newUniqueId());
                r.setEnabled(!prompt.isReadOnly());
                r.setFocusable(!prompt.isReadOnly());

                buttons.add(r);

                if (items.get(i).getValue().equals(s)) {
                    r.setChecked(true);
                }

                r.setOnCheckedChangeListener(this);

                String audioURI = null;
                audioURI = prompt.getSpecialFormSelectChoiceText(items.get(i),
                        FormEntryCaption.TEXT_FORM_AUDIO);

                String imageURI;
                if (items.get(i) instanceof ExternalSelectChoice) {
                    imageURI = ((ExternalSelectChoice) items.get(i)).getImage();
                } else {
                    imageURI = prompt.getSpecialFormSelectChoiceText(items.get(i),
                            FormEntryCaption.TEXT_FORM_IMAGE);
                }

                String videoURI = null;
                videoURI = prompt.getSpecialFormSelectChoiceText(items.get(i),
                        "video");

                String bigImageURI = null;
                bigImageURI = prompt.getSpecialFormSelectChoiceText(
                        items.get(i), "big-image");

                MediaLayout mediaLayout = new MediaLayout(getContext(), player);
                mediaLayout.setAVT(prompt.getIndex(), "." + Integer.toString(i), r, audioURI,
                        imageURI,
                        videoURI, bigImageURI);
                mediaLayout.setAudioListener(this);
                mediaLayout.setPlayTextColor(playColor);
                mediaLayout.setPlayTextBackgroundColor(playBackgroundColor);
                playList.add(mediaLayout);

                if (i != items.size() - 1) {
                    // Last, add the dividing line (except for the last element)
                    ImageView divider = new ImageView(getContext());
                    divider.setBackgroundResource(android.R.drawable.divider_horizontal_bright);
                    mediaLayout.addDivider(divider);
                }
                buttonLayout.addView(mediaLayout);
            }
        }
        buttonLayout.setOrientation(LinearLayout.VERTICAL);

        // The buttons take up the right half of the screen
        addAnswerView(buttonLayout);
    }

    @Override
    public void clearAnswer() {
        for (RadioButton button : this.buttons) {
            if (button.isChecked()) {
                button.setChecked(false);
                clearNextLevelsOfCascadingSelect();
                break;
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
        InputMethodManager inputManager = (InputMethodManager) context
                .getSystemService(Context.INPUT_METHOD_SERVICE);
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

        for (RadioButton button : buttons) {
            if (button.isChecked() && !(buttonView == button)) {
                button.setChecked(false);
                clearNextLevelsOfCascadingSelect();
            }
        }

        SelectChoice choice = items.get((Integer) buttonView.getTag());

        if (choice != null) {
            Collect.getInstance().getActivityLogger().logInstanceAction(this, "onCheckedChanged",
                    choice.getValue(), formEntryPrompt.getIndex());
        } else {
            Collect.getInstance().getActivityLogger().logInstanceAction(this, "onCheckedChanged",
                    "<no matching choice>", formEntryPrompt.getIndex());
        }
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
        for (RadioButton button : this.buttons) {
            button.cancelLongPress();
        }
    }


    public void playNextSelectItem() {
        if (!this.isShown()) {
            return;
        }
        // if there's more, set up to play the next item
        if (playcounter < playList.size()) {
            player.setOnCompletionListener(new OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    resetQuestionTextColor();
                    mediaPlayer.reset();
                    playNextSelectItem();
                }
            });
            // play the current item
            playList.get(playcounter).playAudio();
            playcounter++;

        } else {
            playcounter = 0;
            player.setOnCompletionListener(null);
            player.reset();
        }

    }


    @Override
    public void playAllPromptText() {
        // set up to play the items when the
        // question text is finished
        player.setOnCompletionListener(new OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                resetQuestionTextColor();
                mediaPlayer.reset();
                playNextSelectItem();
            }

        });
        // plays the question text
        super.playAllPromptText();
    }

    @Override
    public void resetQuestionTextColor() {
        super.resetQuestionTextColor();
        for (MediaLayout layout : playList) {
            layout.resetTextFormatting();
        }
    }

}
