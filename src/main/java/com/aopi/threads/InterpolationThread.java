package com.aopi.threads;

import com.aopi.controllers.MainController;
import com.aopi.models.Coordinate;
import com.sun.jna.Callback;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import org.springframework.ui.Model;

import java.util.ArrayList;

public class InterpolationThread  implements Callback {
    private ArrayList<Coordinate> list;
    private Model model;
    private String output;
    private HANDLE interpolationEvent;

    public void callback() {
        if (interpolationEvent != null) {
            interpolate(list);
            MainController.MoreKernel32.instance.SetEvent(interpolationEvent);
            interpolationEvent = null;
        }
        else
        {
            interpolate(list);
        }
    }

    public InterpolationThread(ArrayList<Coordinate> list, Model model) {
        this.list = list;
        this.model = model;
    }

    public InterpolationThread(ArrayList<Coordinate> list, Model model, HANDLE interpolationEvent) {
        this.list = list;
        this.model = model;
        this.interpolationEvent = interpolationEvent;
    }

    private void interpolate(ArrayList<Coordinate> list) {
        try {
            output = "";
            int n = list.size();
            ArrayList<Double> finalcieff = new ArrayList<>();
            for (int l = 0; l < n; l++) {
                finalcieff.add(0.0);
            }
            for (int i = 0; i < n; i++) {
                double denominator = 1;
                ArrayList<Double> control = new ArrayList<>();
                ArrayList<Double> coefficient = new ArrayList<>();
                for (int l = 0; l < n; l++) {
                    coefficient.add(1.0);
                }
                for (int j = 0; j < n; j++) {
                    if (i != j) {
                        denominator = denominator * (list.get(i).abscissa - list.get(j).abscissa);
                        control.add(list.get(j).abscissa);
                    }
                }
                for (int v = 0; v < n; v++) {
                    for (int k = v - 1; k > -1; k--) {
                        if (k == 0) {
                            coefficient.set(k, coefficient.get(k) * -control.get(v - 1));
                        } else if (k == v) {
                            coefficient.set(k, coefficient.get(k) * coefficient.get(k - 1) - control.get(v - 1));
                        } else
                            coefficient.set(k, coefficient.get(k) * -control.get(v - 1) + coefficient.get(k - 1));
                    }
                }
                for (int z = 0; z < n; z++) {
                    if (z == 3) {
                        coefficient.set(z, (coefficient.get(z) / denominator) * list.get(i).ordinate);
                        finalcieff.set(z, finalcieff.get(z) + coefficient.get(z));
                    }
                    else {
                        coefficient.set(z, (coefficient.get(z) / denominator) * list.get(i).ordinate);
                        finalcieff.set(z, finalcieff.get(z) + coefficient.get(z));
                    }
                }

            }
            for (int f = n - 1; f > -1; f--) {
                if (f == (n - 1)) {
                    if (f != 1) output = finalcieff.get(f)+"x^"+f;
                    else output = finalcieff.get(f)+"x";
                } else {
                    switch (f) {
                        case 0:
                            if (finalcieff.get(f) >= 0)
                                output = output + " + " + finalcieff.get(f);
                            else {
                                finalcieff.set(f, Double.parseDouble(finalcieff.get(f).toString().substring(1)));
                                output = output + " - " + finalcieff.get(f);
                            }
                            break;
                        case 1:
                            if (finalcieff.get(f) >= 0)
                                output = output + " + " + finalcieff.get(f) + "x";
                            else {
                                finalcieff.set(f, Double.parseDouble(finalcieff.get(f).toString().substring(1)));
                                output = output + " - " + finalcieff.get(f) + "x";
                            }
                            break;
                        default:
                            if (finalcieff.get(f) >= 0)
                                output = output + " + " + finalcieff.get(f) + "x^" + f;
                            else {
                                finalcieff.set(f, Double.parseDouble(finalcieff.get(f).toString().substring(1)));
                                output = output + " - " + finalcieff.get(f) + "x^" + f;
                            }
                            break;
                    }
                }
            }
            model.addAttribute("function", output);
        }
        catch (NullPointerException e) {
            model.addAttribute("error",
                "Invalid value passed to the Interpolation thread.");
        }
        catch (Exception e) {
            model.addAttribute("error",
                "Unknown error in Interpolation thread.");
        }
    }






}
