package com.alexstyl.specialdates.upcoming;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Handler;

import com.alexstyl.specialdates.date.CelebrationDate;
import com.alexstyl.specialdates.date.ContactEvent;
import com.alexstyl.specialdates.date.Date;
import com.alexstyl.specialdates.date.DateComparator;
import com.alexstyl.specialdates.events.bankholidays.BankHolidaysPreferences;
import com.alexstyl.specialdates.events.namedays.NamedayLocale;
import com.alexstyl.specialdates.events.namedays.NamedayPreferences;
import com.alexstyl.specialdates.events.namedays.NamesInADate;
import com.alexstyl.specialdates.events.namedays.calendar.EasterCalculator;
import com.alexstyl.specialdates.events.namedays.calendar.NamedayCalendar;
import com.alexstyl.specialdates.events.namedays.calendar.resource.NamedayCalendarProvider;
import com.alexstyl.specialdates.service.PeopleEventsProvider;
import com.alexstyl.specialdates.ui.loader.SimpleAsyncTaskLoader;
import com.alexstyl.specialdates.util.ContactsObserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class UpcomingEventsLoader extends SimpleAsyncTaskLoader<List<CelebrationDate>> {

    private static final DateComparator comparator = DateComparator.INSTANCE;

    private final PeopleEventsProvider peopleEventsProvider;
    private final NamedayPreferences namedayPreferences;
    private final Date startingPeriod;
    private final ContactsObserver contactsObserver;
    private final EasterCalculator easterCalculator = new EasterCalculator();
    private final BankHolidaysPreferences bankHolidaysPreferences;

    UpcomingEventsLoader(Context context, PeopleEventsProvider peopleEventsProvider, Date startingPeriod) {
        super(context);
        this.peopleEventsProvider = peopleEventsProvider;
        this.namedayPreferences = NamedayPreferences.newInstance(context);
        this.startingPeriod = startingPeriod;
        this.contactsObserver = new ContactsObserver(getContentResolver(), new Handler());
        this.bankHolidaysPreferences = BankHolidaysPreferences.newInstance(getContext());
        setupContactsObserver();
    }

    @Override
    protected void onUnregisterObserver() {
        super.onUnregisterObserver();
        contactsObserver.unregister();
    }

    @Override
    public List<CelebrationDate> loadInBackground() {

//        TimePeriod firstHalf = new TimePeriod(
//                startingPeriod,
//                Date.endOfYear(startingPeriod.getYear())
//        );
//
//        TimePeriod secondHalf = new TimePeriod(
//                Date.startOfTheYear(startingPeriod.getYear() + 1),
//                startingPeriod.addDay(365)
//        );

        TimePeriod timePeriod = new TimePeriod(
                startingPeriod,
                startingPeriod.addDay(364)
        );

        List<CelebrationDate> allDates = calculateFor(timePeriod);
        Collections.sort(allDates);
        return allDates;
    }

    private List<CelebrationDate> calculateFor(TimePeriod period) {
        List<ContactEvent> contactEvents = peopleEventsProvider.getCelebrationDateFor(period);
        UpcomingDatesBuilder upcomingDatesBuilder = new UpcomingDatesBuilder(period)
                .withContactEvents(contactEvents);

//        if (includesBankholidays()) {
//            GreekBankHolidaysCalculator bankHolidaysCalculator = new GreekBankHolidaysCalculator(easterCalculator);
//            List<BankHoliday> bankHolidays = new BankHolidayProvider(bankHolidaysCalculator)
//                    .getBankHolidayFor(duration);
//            upcomingDatesBuilder.withBankHolidays(bankHolidays);
//        }
//
//        if (includeNamedays()) {
//            List<NamesInADate> namedays = getNamedaysFor(timeDuration);
//            upcomingDatesBuilder.withNamedays(namedays);
//        }

        return upcomingDatesBuilder.build();
    }

    private boolean includesBankholidays() {
        return bankHolidaysPreferences.isEnabled();
    }

    private boolean includeNamedays() {
        return namedayPreferences.isEnabled() && !namedayPreferences.isEnabledForContactsOnly();
    }

    private List<NamesInADate> getNamedaysFor(TimePeriod timeDuration) {
        NamedayLocale selectedLanguage = namedayPreferences.getSelectedLanguage();
        NamedayCalendarProvider namedayCalendarProvider = NamedayCalendarProvider.newInstance(getContext().getResources());
        NamedayCalendar namedayCalendar = namedayCalendarProvider.loadNamedayCalendarForLocale(selectedLanguage, timeDuration.getFrom().getYear());

        Date indexDate = timeDuration.getFrom();
        Date toDate = timeDuration.getTo();
        List<NamesInADate> namedays = new ArrayList<>();

        while (comparator.compare(indexDate, toDate) < 0) {
            NamesInADate allNamedayOn = namedayCalendar.getAllNamedayOn(indexDate);
            if (allNamedayOn.nameCount() > 0) {
                namedays.add(allNamedayOn);
            }
            indexDate = indexDate.addDay(1);
        }
        return namedays;
    }

    private void setupContactsObserver() {
        contactsObserver.registerWith(
                new ContactsObserver.Callback() {
                    @Override
                    public void onContactsUpdated() {
                        onContentChanged();
                    }
                }
        );
    }

    public ContentResolver getContentResolver() {
        return getContext().getContentResolver();
    }

}
