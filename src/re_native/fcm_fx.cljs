(ns re-native.fcm-fx
  (:require [reagent.core :as r]
            [re-frame.core :as re]
            [re-native.core :as rn]
            [re-native.fcm :refer [FCM FCMEventNotification FCMEventRefreshToken]]))

(re/reg-fx
  :fcm-request-permissions
  (fn fcm-request-permissions-fx []
    (.requestPermissions FCM)))

(re/reg-fx
  :fcm-set-badge-number
  (fn fcm-set-badge-number-fx [{:keys [nr]
                                :or   {nr 0}}]
    (.setBadgeNumber FCM (int nr))))

(re/reg-fx
  :fcm-get-token
  (fn fcm-get-token-fx [{:keys [on-success on-failure]
                         :or   {on-success [:fcm-get-token-no-on-success]
                                on-failure [:fcm-get-token-no-on-failure]}}]
    (.then (.getFCMToken FCM)
           (fn fcm-get-token-success-cb [token] (re/dispatch (conj on-success token)))
           (fn fcm-get-token-failure-cb [error] (re/dispatch (conj on-failure error))))))

(re/reg-fx
  :fcm-on-notification
  (fn fcm-on-notification-fx [{:keys [on-local-notification
                                      on-remote-notification
                                      on-tray-notification]
                               :or   {on-local-notification :fcm-on-local-notification-no-on-local-notification
                                      on-remote-notification  :fcm-on-remote-notification-no-on-remote-notification
                                      on-tray-notification :fcm-on-tray-notification-no-on-tray-notification}}]
    (.on FCM FCMEventNotification
         (fn fcm-on-notification-fx-cb [n]
           (let [nclj (js->clj n :keywordize-keys true)
                 aps? (contains? nclj :aps)
                 notification (js->clj (.-notification n) :keywordize-keys true)
                 data (js->clj (.-data n) :keywordize-keys true)
                 opened-from-tray (or (aget n "opened_from_tray") false)
                 remote-notification (or (aget n "remote_notification") false)
                 local-notification (not remote-notification)
                 on-ios? (= rn/os "ios")
                 data2 (if on-ios? (dissoc nclj :finish) nclj)]
             (cond
               remote-notification (re/dispatch (conj on-remote-notification data2))
               local-notification (re/dispatch (conj on-local-notification data2))
               opened-from-tray (re/dispatch (conj on-tray-notification data2))))))))

(re/reg-fx
  :fcm-on-refresh-token
  (fn fcm-on-refresh-token-fx [{:keys [on-refresh-token]
                                :or   {on-refresh-token :fcm-on-refresh-token-no-on-refresh-token}}]
    (.on FCM FCMEventRefreshToken
         (fn fcm-on-refresh-token-fx-cb [t] (re/dispatch (conj on-refresh-token t))))))
