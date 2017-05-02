package propulsar.yonayarit.DomainLayer.Adapters;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import propulsar.yonayarit.DomainLayer.Objects.Benefs;
import propulsar.yonayarit.PresentationLayer.Activities.DetalleBenefActivity;
import propulsar.yonayarit.R;

/**
 * Created by maubocanegra on 09/02/17.
 */

public class BenefsAdapter extends RecyclerView.Adapter<BenefsAdapter.ViewHolder>{

    private ArrayList<Benefs> mDataset;
    Activity activity;

    public BenefsAdapter(ArrayList<Benefs> myDataset, AppCompatActivity activity) {
        mDataset = myDataset;
        this.activity=activity;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_benefs, parent, false);
        // set the view's size, margins, paddings and layout parameters
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.txtTitulo.setText(mDataset.get(position).getTitulo());
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView txtTitulo;

        public ViewHolder(View v) {
            super(v);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(activity, DetalleBenefActivity.class);
                    intent.putExtra("BenefitId",mDataset.get(getLayoutPosition()).getId());
                    activity.startActivity(intent);
                }
            });
            txtTitulo = (TextView)v.findViewById(R.id.item_benefs_titulo);
        }
    }
}
