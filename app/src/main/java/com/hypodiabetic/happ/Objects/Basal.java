package com.hypodiabetic.happ.Objects;

import android.text.format.Time;

/**
 * Created by Tim on 21/08/2015.
 */
public class Basal {

    private int id;
        private Time start;
        private Double rate;

        public Basal() {
            super();
        }

        public Basal(int id, Time start, double rate) {
            super();
            this.id = id;
            this.start = start;
            this.rate = rate;
        }
        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public Time getStart() {
            return start;
        }

        public void setName(Time start) {
            this.start = start;
        }


        public double getRate() {
            return rate;
        }

        public void setRate(double rate) {
            this.rate = rate;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + id;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Basal other = (Basal) obj;
            if (id != other.id)
                return false;
            return true;
        }


}
