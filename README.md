# dbManager
This is the backend server of a non-commercial booking app implementing the core functionality of booking apps like [Calendly](https://calendly.com), [Cal.com](https://cal.com), etc. It does the following basic things:
1. Allows backend users to set up *schedule types* (duration, availability, etc.)
2. Allows public users to view *available slots* which are specified by a schedule type and not in conflict with calendar events.
3. Allows public users to book available slots.

The motivation is very simple. The whole industry of booking apps has become unfriendly towards individual users by starting to charge around $12/mo. Some are even under the disguise of 
- "free plans" (Calendly, limiting 1 schedule type per free user) or 
- "open-source" (Cal.com, "having a GitHub repo"!="open-source" and the paywall kicks in when you start using it). 

Of course, a professional grade booking system can justify its price for businesses and organizations. But as an individual, if you have a very simple need like the above, does the difficulty of the implementation justify the $12/mo? Hopefully this tiny project makes a small difference as a self-hosted free solution for our personal booking needs.

# Note of clarifications
First, a quick explanation of some terms to avoid confusions:
- A **backend user** is a person who uses this backend service and wants a part of their calendar to get booked by the public.
- A **public user** is a person who makes bookings through another frontend or client.
- A **schedule type** is a collection of information including the backend user, the duration, available time of the day, available days of the week, etc. They are uniquely identified by a 6-character string (lower case alphabets+digits, suitable for URL) called *schedule type token*.
- A **time slot** is a time interval generated according a specific schedule type.
- An **available slot** is a time slot that does not conflict with events on the backend user's Google calendar.

For simplicity, the whole app only works with Google calendar for the current version.

# How does it work?
This particular repo is a backend server that handles the following:
1. Create and maintain a database for user records, booking records, schedule types, etc.
2. Receive external requests to add/modify user records, schedule types, etc.
3. Sync with Google calendar and cache the busy time periods.
4. Accept public requests for available slots, and return those that conform to the given schedule type and are available on the Google calendar.
5. Accept public booking requests for available slots.

All these functions are implemented as API endpoints. This should allow for some flexibility of the frontend. A couple examples of how the frontend may look like:
- Full app: a full-fledged UI that allows public users to make bookings as well as backend users to create schedule types.
- Personalized: simple client-side JavaScript that only queries a fix backend user's available slots and makes booking.

# Security
The backend users should not be exposed. Therefore, api endpoints split into two groups (details see the [next section](#overview-of-api-endpoints)).
- Public API endpoints,
- Secured API endpoints (starting with /api/backend). 

For the current version, it uses Basic Authorization header along with a token string generated at the startup of the backend server. For example, when this server runs, it may display a message like the following on console screen (and generate the corresponding item in the "api_token" table in the database).
![](img/server_token_example.png)

If another app wants to communicate with us through a secured api endpoint, it must include the encoded token in a Basic
Authorization header. In this example, you may send a request as follows.

```
curl -X POST http://localhost:8080/api/backend/welcome -H "Authorization: Basic c3ExNHFhZnR0cHBsdDNhYg==" -H "Content-Type: application/json"
```

# Overview of API endpoints

For a detailed documentation of all endpoints,
see [https://bookit-dbmanager-docs.herokuapp.com/](https://bookit-dbmanager-docs.herokuapp.com/)

# Testing instructions

Everything only makes sense after there is at least one backend user with a valid Google API refresh token. Google API
refresh token needs to be obtained via OAuth2 Authorization Code grant which is usually done by browser authorization.
Because we do not have a proper user client, a mock authorization endpoint can be opened up by renaming
the `testEndpoints.kt.examples` into a proper `testEndpoints.kt`. The mock testing scripts are combined in the script
named `teststream.sh`. So a normal testing procedure would be the following.

1. Rename `src/main/kotlin/com/bookit/dbManager/api/testEndpoints.kt.example` to `testEndpoints.kt`.
2. Run the backend server. Go to `/authtest` using your browser and complete the Google authorization. After it is
   successfully done, a valid backend user record would be added to the database, along with the refresh token.
3. Run `src/test/kotlin/com/bookit/dbManager/liveTests/teststream.sh your-email@gmail.com`. It will run a series of
   scripts that visit various important endpoints. The details can be easily understood when opening up
   the `teststream.sh` file.

# Demonstrations

**TODO: upcoming youtube demonstration video**
