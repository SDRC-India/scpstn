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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.SelectMultiData;
import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.form.api.FormEntryPrompt;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.sdrc.scpstn.collect.android.application.Collect;
import org.sdrc.scpstn.collect.android.external.ExternalDataUtil;
import org.sdrc.scpstn.collect.android.external.ExternalSelectChoice;
import org.sdrc.scpstn.collect.android.utilities.TextUtils;
import org.sdrc.scpstn.collect.android.views.MediaLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * SelctMultiWidget handles multiple selection fields using checkboxes.
 *
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */
public class SelectMultiWidget extends QuestionWidget {
    private boolean checkboxInit = true;
    List<SelectChoice> items;

    private ArrayList<CheckBox> checkBoxes;
    ArrayList<MediaLayout> playList;
    private int playcounter = 0;


    @SuppressWarnings("unchecked")
    public SelectMultiWidget(Context context, FormEntryPrompt prompt) {
        super(context, prompt);
        formEntryPrompt = prompt;
        checkBoxes = new ArrayList<CheckBox>();
        playList = new ArrayList<MediaLayout>();

        // SurveyCTO-added support for dynamic select content (from .csv files)
        XPathFuncExpr xpathFuncExpr = ExternalDataUtil.getSearchXPathExpression(
                prompt.getAppearanceHint());
        if (xpathFuncExpr != null) {
            items = ExternalDataUtil.populateExternalChoices(prompt, xpathFuncExpr);
        } else {
            items = prompt.getSelectChoices();
        }

        List<Selection> ve = new ArrayList<Selection>();
        if (prompt.getAnswerValue() != null) {
            ve = (List<Selection>) prompt.getAnswerValue().getValue();
        }

        LinearLayout answerLayout = new LinearLayout(getContext());
        answerLayout.setOrientation(LinearLayout.VERTICAL);
        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                String choiceName = prompt.getSelectChoiceText(items.get(i));
                CharSequence choiceDisplayName;
                if (choiceName != null) {
                    choiceDisplayName = TextUtils.textToHtml(choiceName);
                } else {
                    choiceDisplayName = "";
                }
                // no checkbox group so id by answer + offset
                CheckBox c = new CheckBox(getContext());
                c.setTag(Integer.valueOf(i));
                c.setId(QuestionWidget.newUniqueId());
                c.setText(choiceDisplayName);
                c.setMovementMethod(LinkMovementMethod.getInstance());
                c.setTextSize(TypedValue.COMPLEX_UNIT_DIP, answerFontsize);
                c.setFocusable(!prompt.isReadOnly());
                c.setEnabled(!prompt.isReadOnly());

                for (int vi = 0; vi < ve.size(); vi++) {
                    // match based on value, not key
                    if (items.get(i).getValue().equals(ve.get(vi).getValue())) {
                        c.setChecked(true);
                        break;
                    }

                }
                checkBoxes.add(c);
                // when clicked, check for readonly before toggling
                c.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (!checkboxInit && formEntryPrompt.isReadOnly()) {
                            if (buttonView.isChecked()) {
                                buttonView.setChecked(false);
                                Collect.getInstance().getActivityLogger().logInstanceAction(this,
                                        "onItemClick.deselect",
                                        items.get((Integer) buttonView.getTag()).getValue(),
                                        formEntryPrompt.getIndex());
                            } else {
                                buttonView.setChecked(true);
                                Collect.getInstance().getActivityLogger().logInstanceAction(this,
                                        "onItemClick.select",
                                        items.get((Integer) buttonView.getTag()).getValue(),
                                        formEntryPrompt.getIndex());
                            }
                        }
                    }
                });

                String audioURI = null;
                audioURI =
                        prompt.getSpecialFormSelectChoiceText(items.get(i),
                                FormEntryCaption.TEXT_FORM_AUDIO);

                String imageURI;
                if (items.get(i) instanceof ExternalSelectChoice) {
                    imageURI = ((ExternalSelectChoice) items.get(i)).getImage();
                } else {
                    imageURI = prompt.getSpecialFormSelectChoiceText(items.get(i),
                            FormEntryCaption.TEXT_FORM_IMAGE);
                }

                String videoURI = null;
                videoURI = prompt.getSpecialFormSelectChoiceText(items.get(i), "video");

                String bigImageURI = null;
                bigImageURI = prompt.getSpecialFormSelectChoiceText(items.get(i), "big-image");

                MediaLayout mediaLayout = new MediaLayout(getContext(), player);
                mediaLayout.setAVT(prompt.getIndex(), "." + Integer.toString(i), c, audioURI,
                        imageURI, videoURI, bigImageURI);

                playList.add(mediaLayout);

                // Last, add the dividing line between elements (except for the last element)
                if (i != items.size() - 1) {
                    ImageView divider = new ImageView(getContext());
                    divider.setBackgroundResource(android.R.drawable.divider_horizontal_bright);
                    mediaLayout.addDivider(divider);
                }
                answerLayout.addView(mediaLayout);
            }
            addAnswerView(answerLayout);
        }

        checkboxInit = false;

    }


    @Override
    public void clearAnswer() {
        for (CheckBox c : checkBoxes) {
            if (c.isChecked()) {
                c.setChecked(false);
            }
        }
    }


    @Override
    public IAnswerData getAnswer() {
        List<Selection> vc = new ArrayList<Selection>();
        for (int i = 0; i < checkBoxes.size(); ++i) {
            CheckBox c = checkBoxes.get(i);
            if (c.isChecked()) {
                vc.add(new Selection(items.get(i)));
            }
        }

        if (vc.size() == 0) {
            return null;
        } else {
            return new SelectMultiData(vc);
        }

    }


    @Override
    public void setFocus(Context context) {
        // Hide the soft keyboard if it's showing.
        InputMethodManager inputManager =
                (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(this.getWindowToken(), 0);
    }


    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        for (CheckBox c : checkBoxes) {
            c.setOnLongClickListener(l);
        }
    }


    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        for (CheckBox c : checkBoxes) {
            c.cancelLongPress();
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
