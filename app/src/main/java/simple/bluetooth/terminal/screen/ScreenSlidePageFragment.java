package simple.bluetooth.terminal.screen;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import li.garteroboter.pren.R;


/**
 * Very basic PageFragment, which can be a Fundament on which to build more complex structures.
 */
public class ScreenSlidePageFragment extends Fragment {
    private static final String ARG_PARAM1 = "param1";

    private String mParam1;
    private TextView myTextView;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
        }
    }

    public static ScreenSlidePageFragment newInstance(String param1) {
        ScreenSlidePageFragment fragment = new ScreenSlidePageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_screen_slide_page, container, false);
        /*
        myTextView = (TextView) view.findViewById(R.id.exampleText);
        myTextView.setText(mParam1);

         */
        return view;

    }

    public ScreenSlidePageFragment() {
        // Required empty public constructor
    }
}

