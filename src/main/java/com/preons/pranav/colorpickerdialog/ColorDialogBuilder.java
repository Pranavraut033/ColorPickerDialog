package com.preons.pranav.colorpickerdialog;

import android.animation.FloatEvaluator;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EmptyStackException;

import androidx.annotation.ColorInt;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import pranav.utilities.Log;
import pranav.utilities.Utilities;

/**
 * {@link ColorDialogBuilder} is use to create a color picker dialog
 * it currently supports three modes.
 * <p>
 * 1 : {@link ColorDialogBuilder.Mode#TAP}<br>
 * 2 : {@link ColorDialogBuilder.Mode#SINGLE_SELECTION}<br>
 * 3 : {@link ColorDialogBuilder.Mode#MULTI_SELECTION}<br>
 * </P>
 * <p>
 * <P><font color = 'red'>
 * <b>PS:</b> It is highly Recommended to use the method offers by {@link ColorDialogBuilder}
 * and not by the parent class {@link AlertDialog.Builder}
 * </font></P>
 *
 * @author Pranav Raut
 * @version 1.0
 * @since 1.0 (24-07-2017)
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class ColorDialogBuilder<D extends AlertDialog> extends AlertDialog.Builder {

    /**
     * Constant to indicate <b>circular<b/> button.
     */
    public static final int CIRCULAR = 0x862;

    /**
     * Constant to indicate <b>square</b> button.
     */
    public static final int SQUARE = 0x7c4;

    private static final TimeInterpolator interpolator = new OvershootInterpolator();
    /**
     * use this duration (225 milliseconds)if you want to animate externally
     */
    private static final long A = 0xe1;

    /**
     * base layout to which the view is added
     */
    private final View rootLayout;
    private final TextView t;
    private Context c;
    private Log log = new Log();

    /**
     * holds the given color
     */
    private ArrayList<Integer> colors = new ArrayList<>();
    /**
     * check {@link ColorDialogBuilder#setSecondaryColors(Integer[][])}
     */
    private ArrayList<ArrayList<Integer>> secondaryColors = new ArrayList<>();
    private ArrayList<Listener> listeners = new ArrayList<>();
    private ArrayList<DialogListener> dialogListeners = new ArrayList<>();

    /**
     * Holds number of column.
     */
    private int columns = 5;
    /**
     * The mode by the the dialog functions.
     *
     * @see ColorDialogBuilder.Mode
     */
    private Mode mode = Mode.TAP;
    /**
     * Holds the current style of button
     *
     * @see ColorDialogBuilder#setType(int)
     */
    private int type = CIRCULAR;

    /**
     * The number of color that are added to dialog view<br>
     * This overrides the size of {@link ColorDialogBuilder#colors}.<br>
     * If the numColor is greater then {@link ColorDialogBuilder#colors}.size() the extra fields
     * Is filled with {@link Color#WHITE}.<br>
     * If the {@link ColorDialogBuilder#colors}.size() == 0 the all the fields is
     * filled with {@link Color#WHITE}.<br>
     * If {@link ColorDialogBuilder#colors}.size() greater than numColor then numColor is updated.
     */
    private int numColor;

    /**
     * Flag for weather the the {@link ColorDialogBuilder#colors} is changed or not
     */
    private boolean changed;

    /**
     * holds the selected color when {@link ColorDialogBuilder#mode}
     * is {@link ColorDialogBuilder.Mode#MULTI_SELECTION}.
     */
    private ArrayList<Integer> s = new ArrayList<>();
    private ArrayList<View> vs = new ArrayList<>();
    private ArrayList<ImageButton> buttons = new ArrayList<>();


    /**
     * The object of {@link D} to which the {@link AlertDialog} return by {@link ColorDialogBuilder#create()} is assigned.
     * When the @{@link ColorDialogBuilder.Mode Mode} is set to {@link ColorDialogBuilder.Mode#TAP}
     * this is to use to dismiss the {@link AlertDialog} after the color is selected.
     */
    @Nullable
    private D target;
    private boolean animateOnTap;
    private Integer lastColor;
    private int lK;
    private Integer mainSelected;
    /**
     * {@link View#setOnClickListener(View.OnClickListener) onClick} for every color.<br>
     * It calls the interface {@link ColorDialogBuilder.Listener} which gives the current
     * selected Color value.<br>
     * The function of the <b>onClick</b> is controlled by {@link ColorDialogBuilder.Mode}.
     */

    private View.OnClickListener l = new View.OnClickListener() {
        View lastView;

        @Override
        public void onClick(View v) {
            int k = v.getId();
            Integer color = colors.get(k);
            if (color == null) {
                log.w("Color selected have null value", new NullPointerException());
                return;
            }
            for (Listener listener : listeners)
                if (mode == Mode.MULTI_SELECTION && listener instanceof Listener.MultiSelection) {
                    boolean b = s.indexOf(color) != -1;
                    animate(v, false, !b);
                    if (b) s.remove(color);
                    else s.add(color);
                    vs.add(v);
                    log.d(mode + ": onClick: selected items:\n" + s.toString());
                    ((Listener.MultiSelection) listener).onMultiSelect(s.toArray(new Integer[0]), color);
                } else if (mode == Mode.SINGLE_SELECTION && listener instanceof Listener.SingleSelection) {
                    if (lastView != null) {
                        lastView.animate().scaleX(1).scaleY(1).setDuration(A).setInterpolator(interpolator).start();
                        lastView = v;
                    }
                    v.animate().scaleX(1.2f).scaleY(1.2f)
                            .setDuration(A).setInterpolator(interpolator).start();
                    ColorDialogBuilder.this.lastColor = color;
                    lK = k;
                    log.d(mode + ": onClick: " + Integer.toHexString(color) + " Selected at index " + k);
                } else if (mode == Mode.SECONDARY_SELECTION && listener instanceof Listener.SecondarySelection) {
                    if (mainSelected == null) {
                        create(colors = secondaryColors.get(k));
                        mainSelected = color;
                    } else tap(listener, v);

                    log.d(mode + ": onClick: " + Integer.toHexString(color) + " Selected at index " + k);
                } else if (mode == Mode.TAP && listener instanceof Listener.Tap) {
                    tap(listener, v);
                    log.d(mode + ": onClick: " + Integer.toHexString(color) + " Selected at index " + k);
                }
            if (listeners.isEmpty())
                log.w("No Listener added, skipping onClick changes", new EmptyStackException());
        }
    };


    @SuppressLint("InflateParams")
    public ColorDialogBuilder(Context context) {
        super(context);
        this.c = context;
        rootLayout = LayoutInflater.from(c).inflate(R.layout.button_layout, null);
        t = rootLayout.findViewById(R.id.alertTitle);
    }

    /**
     * This method has <b><font color='#ff000000'>No Effect</font></b>, as the view
     * is dynamically added when {@link ColorDialogBuilder#create()} is called.
     *
     * @return <font color = 'blue'>NULL</font>
     */
    @Deprecated
    @Nullable
    public AlertDialog.Builder setView(int layoutResId) {
        return null;
    }

    /**
     * This method has <b><font color='#ff000000'>No Effect</font></b>.
     *
     * @return <font color = 'blue'>NULL</font>
     * @see ColorDialogBuilder#setView(int) setView()
     */
    @Deprecated
    public AlertDialog.Builder setView(View v) {
        return null;
    }

    /**
     * This Method is use to change the functioning of {@link ColorDialogBuilder#l}
     * when a color is pressed.
     *
     * @param mode can be one of the this {@link ColorDialogBuilder.Mode#TAP},
     *             {@link ColorDialogBuilder.Mode#SINGLE_SELECTION},
     *             {@link ColorDialogBuilder.Mode#MULTI_SELECTION}.
     * @return currently created object of {@link ColorDialogBuilder}.
     */
    public ColorDialogBuilder<D> setMode(Mode mode) {
        this.mode = mode;
        if (mode == Mode.SINGLE_SELECTION || mode == Mode.MULTI_SELECTION) {
            super.setPositiveButton("DONE", (dialog, which) -> {

                for (Listener listener : listeners) {
                    if (listener instanceof Listener.SingleSelection)
                        ((Listener.SingleSelection) listener).onSingleSelect(lastColor, lK);
                    if (listener instanceof Listener.MultiSelection) {
                        ((Listener.MultiSelection) listener).onMultiSelect(s.toArray(new Integer[0]), lK);
                    }
                }
                for (View v : vs) {
                    v.setScaleX(1);
                    v.setScaleY(1);
                    v.setAlpha(1);
                }

                vs.clear();
                s.clear();

                lK = -1;
                lastColor = null;

                dialog.dismiss();
            });
            super.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        } else {
            Button button = target != null ? target.getButton(AlertDialog.BUTTON_NEGATIVE) : null;
            if (button != null) button.setVisibility(View.GONE);
            button = target != null ? target.getButton(AlertDialog.BUTTON_POSITIVE) : null;
            if (button != null) button.setVisibility(View.GONE);
            button = target != null ? target.getButton(AlertDialog.BUTTON_NEUTRAL) : null;
            if (button != null) button.setVisibility(View.GONE);
        }
        return this;
    }

    public void refresh() {
        ArrayList<Integer> integers = new ArrayList<>(colors);
        for (int i = 0; i < integers.size(); i++) if (integers.get(i) == null) integers.remove(i);
        for (int i = 0; i < buttons.size(); i++)
            buttons.get(i).setColorFilter(integers.get(i), PorterDuff.Mode.SRC_ATOP);
        changed = false;
    }

    /**
     * @param count Check {@link ColorDialogBuilder#numColor} for description.
     */
    public ColorDialogBuilder<D> setNumCount(int count) {
        this.numColor = count;
        return this;
    }

    /**
     * This method is used to create a {@link D Dialog}.<br>
     * <b>Hint: Use {@link ColorDialogBuilder#refresh()} if the color are updated after create()
     * i.e after the dialog is created.</b>
     *
     * @see AlertDialog#create()
     */
    @Override
    public D create() {
        super.setView(rootLayout);

        create(colors);

        //noinspection unchecked
        target = (D) super.create();

        if (target != null) {
            target.setOnDismissListener(dialog -> {
                if (mode == Mode.SECONDARY_SELECTION) {
                    colors = new ArrayList<>();
                    for (int i = 0; i < secondaryColors.size(); i++)
                        colors.add(i, secondaryColors.get(i).get(0));
                    mainSelected = null;
                    create(colors);
                }
                for (DialogListener listener : dialogListeners)
                    if (listener instanceof DialogListener.Dismiss)
                        ((DialogListener.Dismiss) listener).onDismiss(dialog);
            });

            target.setOnCancelListener(dialog -> {
                for (DialogListener listener : dialogListeners)
                    if (listener instanceof DialogListener.Cancel)
                        ((DialogListener.Cancel) listener).onCancel(dialog);
            });

            target.setOnShowListener(dialog -> {
                for (DialogListener listener : dialogListeners)
                    if (listener instanceof DialogListener.Show)
                        ((DialogListener.Show) listener).onShow(dialog);
                if (changed)
                    refresh();
            });
        }

        return target;
    }

    private void create(ArrayList<Integer> integers) {

        GridLayout gridLayout = rootLayout.findViewById(R.id.root);
        gridLayout.removeAllViews();

        for (int i = 0; i < integers.size(); i++) if (integers.get(i) == null) integers.remove(i);

        int cl, l, row = (l = integers.size()) / (cl = getColumns()) + 1;
        int t = getType() == SQUARE ? R.style.square : R.style.circular;
        if (l == 0) {
            l = numColor;
            integers = new ArrayList<>(l);
            Collections.fill(integers, -1);
        } else if (l < numColor) {
            ArrayList<Integer> f = new ArrayList<>(numColor - l);
            Collections.fill(f, -1);
            integers.addAll(f);
        } else {
            numColor = l;
        }
        gridLayout.setColumnCount(cl);
        gridLayout.setRowCount(row);

        for (int i = 0, cc = 0, rr = 0; i < l; i++) {
            GridLayout.Spec sR = GridLayout.spec(rr);
            GridLayout.Spec sC = GridLayout.spec(cc);
            Utilities.Resources res = new Utilities.Resources(getContext());
            ImageButton b = new ImageButton(new ContextThemeWrapper(c, t), null, t);
            b.setId(i);
            b.setLayoutParams(new GridLayout.LayoutParams(sR, sC));
            b.setOnClickListener(this.l);
            b.setColorFilter(integers.get(i), PorterDuff.Mode.SRC_ATOP);

            gridLayout.addView(b);


            if (cc == cl - 1) {
                rr++;
                cc = 0;
            } else
                cc++;
            buttons.add(b);
        }
    }

    @Nullable
    public D getTarget() {
        return target;
    }

    /**
     * @see ColorDialogBuilder#setColors(String...)
     */
    public ColorDialogBuilder<D> setColors(@ColorInt Integer... colors) {
        changed = true;
        this.colors = new ArrayList<>();
        Collections.addAll(this.colors, colors);
        return this;
    }

    /**
     * @param colors Add color to existing {@link ArrayList} of {@link ColorDialogBuilder#colors}.
     * @return Current object of class {@link ColorDialogBuilder}
     */
    public ColorDialogBuilder<D> addColors(@ColorInt Integer... colors) {
        Collections.addAll(this.colors, colors);
        changed = colors.length == 0;
        return this;
    }

    public ColorDialogBuilder<D> addColors(@Size(min = 1) String... colors) {
        for (String s : colors) this.colors.add(Color.parseColor(s));
        changed = colors.length == 0;
        return this;
    }

    public ColorDialogBuilder<D> updateColor(@ColorInt Integer color, int index) {
        Integer[] integers = getColors();
        colors.remove(index);
        colors.add(index, color);
        changed = !Arrays.equals(getColors(), integers);
        return this;
    }

    public ColorDialogBuilder<D> updateColor(@Size(min = 1) String color, int index) {
        return updateColor(Color.parseColor(color), index);
    }

    /**
     * Use this only if You selected {@link ColorDialogBuilder.Mode} to {@link ColorDialogBuilder.Mode#SECONDARY_SELECTION}
     *
     * @param colors set of colors as array <br>
     *               eg:<br>Integer[][] colors = new Integer[][] {<br>
     *               {<B>Primary Colors</B>, Sub colors...},<br>
     *               {<B>Primary Colors</B>, Sub colors...},<br>
     *               .<br>.<br>.<br>};<br>
     *               where <b>Primary Color</b> are place the color visible of main screen
     *               and <i>sub colors</i> are shown on next screen.<BR><BR>
     *               <B>HINT: It should also contain primary colors</B>
     */
    public ColorDialogBuilder<D> setSecondaryColors(@ColorInt Integer[][] colors) {
        for (Integer[] ints : colors) {
            ArrayList<Integer> integers = new ArrayList<>();
            Collections.addAll(integers, ints);
            secondaryColors.add(integers);
        }
        this.colors = new ArrayList<>();
        for (int i = 0; i < colors.length; i++)
            this.colors.add(i, secondaryColors.get(i).get(0));
        return this;
    }

    /**
     * Use this only if You selected {@link ColorDialogBuilder}
     *
     * @param colors set of colors as array <br>
     *               eg:<br>String[][] colors = new String[][] {<br>
     *               {<B>Primary Colors</B>, Sub colors...},<br>
     *               {<B>Primary Colors</B>, Sub colors...},<br>
     *               .<br>.<br>.<br>};<br>
     *               <font color='red'>NOTE: </font>
     *               where <b>Primary Color</b> are place the color visible of main screen
     *               and <i>sub colors</i> are shown on next screen.
     */
    public ColorDialogBuilder<D> setSecondaryColors(@Size(min = 1) String[][] colors) {
        for (String[] strings : colors) {
            ArrayList<Integer> integers = new ArrayList<>();
            for (String s : strings) integers.add(Color.parseColor(s));
            secondaryColors.add(integers);
        }
        return this;
    }

    @NonNull
    @ColorInt
    public Integer[] getColors() {
        return colors.toArray(new Integer[0]);
    }

    /**
     * This method replace the existing colors of the color picker
     *
     * @param colors colors to <B>set</B> in dialog
     * @return Current object of class {@link ColorDialogBuilder}
     */
    public ColorDialogBuilder<D> setColors(@Size(min = 1) String... colors) {
        changed = true;
        this.colors = new ArrayList<>();
        for (String s : colors) this.colors.add(Color.parseColor(s));
        return this;
    }

    @Override
    public ColorDialogBuilder<D> setTitle(CharSequence titleText) {
        t.setText(titleText);
        return this;
    }

    @Override
    public ColorDialogBuilder<D> setTitle(@StringRes int titleId) {
        t.setText(titleId);
        return this;
    }

    /**
     * @return The current style of buttons can be
     * {@link ColorDialogBuilder#SQUARE} or {@link ColorDialogBuilder#CIRCULAR}.
     */
    public int getType() {
        return type;
    }

    /**
     * @param type Set buttons style can be
     *             {@link ColorDialogBuilder#SQUARE} or {@link ColorDialogBuilder#CIRCULAR}.
     */
    public ColorDialogBuilder<D> setType(@y int type) {
        this.type = type;
        return this;
    }

    /**
     * @return Number of columns.
     */
    public int getColumns() {
        return columns;
    }

    public ColorDialogBuilder<D> setColumns(int columns) {
        this.columns = columns;
        return this;
    }

    public ColorDialogBuilder<D> addListeners(Listener... listeners) {
        Collections.addAll(this.listeners, listeners);
        return this;
    }

    public ColorDialogBuilder<D> removeListeners(Listener... listeners) {
        this.listeners.removeAll(Arrays.asList(listeners));
        return this;
    }

    /**
     * @return The number of color selected if mode is set to {@link ColorDialogBuilder.Mode#MULTI_SELECTION}
     * else <b>0</b>.
     */
    public int getColorSelectionCount() {
        return s.size();
    }

    public ColorDialogBuilder<D> addDialogListeners(DialogListener... listeners) {
        this.dialogListeners = new ArrayList<>();
        Collections.addAll(this.dialogListeners, listeners);
        return this;
    }

    public ColorDialogBuilder<D> removeDialogListeners(DialogListener... listeners) {
        this.dialogListeners.removeAll(Arrays.asList(listeners));
        return this;
    }

    public View getRootLayout() {
        return rootLayout;
    }

    public boolean isChanged() {
        return changed;
    }

    /**
     * @return true if logging is enabled.
     * @see ColorDialogBuilder#setLoggingEnabled(boolean)
     */
    public boolean isLoggingEnabled() {
        return log.isLoggingEnabled();
    }

    /**
     * @param loggingEnabled Flag to enable logging.
     */
    public ColorDialogBuilder<D> setLoggingEnabled(boolean loggingEnabled) {
        log.setLoggingEnabled(loggingEnabled);
        return this;
    }

    public ColorDialogBuilder<D> setAnimateOnTap(boolean animateOnTap) {
        this.animateOnTap = animateOnTap;
        return this;
    }

    private void tap(final Listener listener, View v) {
        final int k = v.getId();
        new Handler().postDelayed(() -> {
            if (listener instanceof Listener.Tap)
                ((Listener.Tap) listener).onTap(colors.get(k), k);
            else if (listener instanceof Listener.SecondarySelection)
                ((Listener.SecondarySelection) listener).onSecondarySelect(colors.get(k), mainSelected);
            if (target != null) target.dismiss();
        }, A + 25);
        animate(v, true, false);
    }

    private void animate(View v, boolean t, boolean tog) {
        if (t) {
            ObjectAnimator.ofObject(v, "scaleY", new FloatEvaluator(), 1, 1.2f, 1)
                    .setDuration(A - 25).start();
            ObjectAnimator.ofObject(v, "scaleX", new FloatEvaluator(), 1, 1.2f, 1)
                    .setDuration(A - 25).start();
        } else v.animate().scaleX(tog ? .8f : 1).scaleY(tog ? .8f : 1).alpha(tog ? .95f : 1)
                .setInterpolator(interpolator).setDuration(A).start();
    }

    public enum Mode {
        /**
         * When color is tapped {@link ColorDialogBuilder#create() dialog} will called the {@link ColorDialogBuilder.Listener}
         * and gives single selected color as parameter
         * dismissing the {@link ColorDialogBuilder#target}.
         */
        TAP,
        /**
         * In this Mode a color is selected via {@link View.OnClickListener OnClickListener}.
         * The {@link ColorDialogBuilder.Listener.SingleSelection#onSingleSelect(Integer, int)} is called
         * when {@link ColorDialogBuilder#target} is dismiss The listener will provide the selected color
         */
        SINGLE_SELECTION,
        /**
         * In this Mode User can select multi Colors from the {@link ColorDialogBuilder#colors}
         * the select is return via {@link ColorDialogBuilder.Listener.MultiSelection#setSecondaryColors(Integer[][])}
         * when the dialog is dismissed
         *
         * @see ColorDialogBuilder.Listener.MultiSelection#onMultiSelect(Integer[], int)
         */
        MULTI_SELECTION,
        /**
         * When a Color is {@link View.OnClickListener clicked} a new dialog is shown with all the sub colors defined.
         *
         * @see ColorDialogBuilder#setSecondaryColors(Integer[][])
         */
        SECONDARY_SELECTION
    }

    /**
     * Common {@link AlertDialog}'s listener recreated so that it can have multiple {@link DialogInterface.OnShowListener},
     * {@link DialogInterface.OnDismissListener} and {@link DialogInterface.OnCancelListener}
     * individually or Simultaneously using {@link ColorDialogBuilder.DialogListener.Events Events}
     */
    public interface DialogListener {

        interface Dismiss extends DialogListener {
            void onDismiss(DialogInterface dialog);
        }

        interface Cancel extends DialogListener {
            void onCancel(DialogInterface dialog);
        }

        interface Show extends DialogListener {
            void onShow(DialogInterface dialog);
        }

        interface Events extends Dismiss, Cancel, Show {
        }

    }

    /**
     * Listeners for color selection.
     *
     * @see ColorDialogBuilder.Listener.Tap#onTap(Integer color, int)
     * @see ColorDialogBuilder.Listener.SingleSelection#onSingleSelect(Integer color, int)
     * @see ColorDialogBuilder.Listener.MultiSelection#onMultiSelect(Integer[] colors, int)
     * @see ColorDialogBuilder.Listener.SecondarySelection#onSecondarySelect(Integer, Integer)
     * @see ColorDialogBuilder.Listener.ViewClick#onClick(View, Integer color)
     */
    public interface Listener {

        /**
         * Check {@link ColorDialogBuilder.Listener.Tap#onTap(Integer, int)}
         * for description.
         */
        interface Tap extends Listener {
            /**
             * Listener corresponds to {@link ColorDialogBuilder.Mode#TAP}
             * This method is called when The mode is set to {@link ColorDialogBuilder.Mode#TAP}.
             *
             * @param index The index of the color
             * @param color The color itself which was Tap
             */
            void onTap(Integer color, int index);
        }

        /**
         * Check {@link ColorDialogBuilder.Listener.SingleSelection#onSingleSelect(Integer, int)}
         * for description.
         */
        interface SingleSelection extends Listener {
            /**
             * Listener corresponds to {@link ColorDialogBuilder.Mode#SINGLE_SELECTION}.
             * This method is called when The mode is set to {@link ColorDialogBuilder.Mode#SINGLE_SELECTION}.
             *
             * @param selectedColor The selected color
             * @param index         The index of the color selecting
             */
            void onSingleSelect(Integer selectedColor, int index);
        }

        /**
         * Check {@link ColorDialogBuilder.Listener.MultiSelection#onMultiSelect(Integer[], int)}
         * for description.
         */
        interface MultiSelection extends Listener {
            /**
             * Listener corresponds to {@link ColorDialogBuilder.Mode#MULTI_SELECTION}.<br>
             * This method is called when The mode is set to {@link ColorDialogBuilder.Mode#MULTI_SELECTION}.
             *
             * @param selection   The Collection of color Selected.
             * @param recentColor The most recent color selected.
             */
            void onMultiSelect(Integer[] selection, int recentColor);
        }

        interface SecondarySelection extends Listener {
            /**
             * @param selectedColor
             * @param primaryColor
             */
            void onSecondarySelect(Integer selectedColor, Integer primaryColor);
        }


        interface ViewClick extends Listener {
            void onClick(View view, Integer color);
        }

        /**
         * Used this Interface if the user wants to try all the mode one by one without rewriting
         * the {@link ColorDialogBuilder#addListeners(Listener...)} code
         * this save lot af time while developing.
         */
        interface Events extends Tap, SingleSelection, MultiSelection, SecondarySelection {
        }
    }

    @IntDef(value = {SQUARE, CIRCULAR})
    private @interface y {
    }
}