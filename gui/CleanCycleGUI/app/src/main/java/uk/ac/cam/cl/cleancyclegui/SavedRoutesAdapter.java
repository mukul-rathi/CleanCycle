package uk.ac.cam.cl.cleancyclegui;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * An adapter for a recycler view used to display the saved routes.
 */
public class SavedRoutesAdapter extends RecyclerView.Adapter<SavedRoutesAdapter.RoutesViewHolder> {
    public static class RoutesViewHolder extends RecyclerView.ViewHolder {
        public View textView;
        public RoutesViewHolder(View textView) {
            super(textView);
            this.textView = textView;
        }

        public void bind(String key, OnItemClickListener listener) {
            textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onItemClick(key);
                }
            });
            textView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    listener.onItemLongClick(key);
                    return true;
                }
            });
        }
    }

    private SavedRoutes savedRoutes;
    private OnItemClickListener itemClickListener;

    public SavedRoutesAdapter(SavedRoutes savedRoutes, OnItemClickListener itemClickListener) {
        this.savedRoutes = savedRoutes;
        this.itemClickListener = itemClickListener;
    }

    @NonNull
    @Override
    public RoutesViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View textHolder = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.route_item, viewGroup, false);
        RoutesViewHolder vh = new RoutesViewHolder(textHolder);
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull RoutesViewHolder routesViewHolder, int i) {
        try {
            String key = savedRoutes.getNames().get(i);
            routesViewHolder.bind(key, itemClickListener);
            TextView txtView = routesViewHolder.textView.findViewById(R.id.text_holder);
            txtView.setText(key);
        } catch (RoutesNotLoadedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return savedRoutes.numberOfRoutes();
    }
}
