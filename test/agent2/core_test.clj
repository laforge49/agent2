(ns agent2.core-test
  (:require [clojure.test :refer :all]
            [agent2.core :refer :all]))


(defn catcher
  [f]
  (try (f)
       nil
       (catch Throwable t t)))

(defn inc-state [agent-value ctx-atom]
  (set-agent-value! ctx-atom (+ 1 agent-value))
  )

(defn return-state
  [agent-value ctx-atom]
  (reply ctx-atom agent-value))

(def a22 (agent 22))
(signal a22 inc-state)
(def r22 (request-call a22 return-state))
(deftest promise-call
  (is (= 23 r22)))

(defn ignore-result [result])
(def a33 (agent 33))
(defn check33 [agent-value ctx-atom]
  (request ctx-atom a33 return-state () ignore-result)
  (request ctx-atom a33 return-state () ignore-result)
  (request ctx-atom a33 return-state () ignore-result)
  )
(def a34 (agent 34))
(def r34 (.getMessage (catcher #(request-call a34 check33))))
(deftest missing-call2
  (is (= r34 "Missing response")))

(def a43 (agent 43))
(defn check43 [agent-value ctx-atom]
  (set-max-requests! ctx-atom 2)
  (request ctx-atom a43 return-state () ignore-result)
  (request ctx-atom a43 return-state () ignore-result)
  (request ctx-atom a43 return-state () ignore-result)
  (reply ctx-atom 999)
  )
(def a44 (agent 44))
(def r44 (.getMessage (catcher #(request-call a44 check43))))
(deftest too-many-requests
  (is (= r44 "Exceeded max requests")))

(def a53 (agent 53))
(defn check53 [agent-value ctx-atom]
  (reduce-request-depth! ctx-atom 0)
  (request ctx-atom a53 return-state () ignore-result)
  (request ctx-atom a53 return-state () ignore-result)
  (request ctx-atom a53 return-state () ignore-result)
  (reply ctx-atom 999)
  )
(def a54 (agent 54))
(def r54 (.getMessage (catcher #(request-call a54 check53))))
(deftest too-many-requests
  (is (= r54 "Exceeded request depth")))

(defn dbz-snt [agent-value]
  (/ 0 0))

(defn dbz-req [agent-value ctx-atom]
  (/ 0 0))

(defn dbz-rsp [result]
  (/ 0 0))

(def p2 (promise))
(defn eh2 [a e]
  (deliver p2 true)
  )
(def a2 (agent true :error-handler eh2))
(signal a2 dbz-req)
(def q2 (deref p2 200 false))
(deftest signal-error-handler-2
  (is (= true q2)))

(def p3 (promise))
(defn eh3 [a e]
  (deliver p3 "got error"))
(defn eh3b [e]
  (.printStackTrace e))
(def a3 (agent true :error-handler eh3))
(def b3 (agent "Fred" :error-handler eh3b))
(signal a3
        (fn [agent-value ctx-atom]
          (request ctx-atom
                   b3
                   dbz-req ()
                   '(println 99))))
(def q3 (deref p3 200 "timeout"))
(deftest request-error-handler-3
  (is (= q3 "got error")))

(def p4 (promise))
(defn eh4 [a e]
  (deliver p4 "got error"))
(def a4 (agent true :error-handler eh4))
(def b4 (agent "Fred"))
(signal a4
        (fn [agent-value ctx-atom]
          (request ctx-atom
                   b4
                   (fn [agent-value ctx-atom] (reply ctx-atom 42)) ()
                   dbz-rsp)))
(def q4 (deref p4 200 nil))
(deftest reply-error-handler-4
  (is (= q4 "got error")))

(defn eh5 [a e]
  (.printStackTrace e))
(def p5 (promise))
(defn exh5 [e]
  (deliver p5 "got exception"))
(def a5 (agent "Sam" :error-handler eh5))
(signal a5 (fn [agent-value ctx-atom]
             (set-exception-handler! ctx-atom exh5)
             (/ 0 0)))
(def q5 (deref p5 1200 "timeout"))
(deftest signal-exception-handler-5
  (is (= "got exception" q5)))

(def p6 (promise))
(defn eh6 [a e]
  (.printStackTrace e)
  (deliver p6 "got error"))
(defn exh6 [e]
  (deliver p6 "got exception"))
(def a6 (agent true :error-handler eh6))
(def b6 (agent "Fred"))
(signal a6
        (fn [agent-value ctx-atom]
          (set-exception-handler! ctx-atom exh6)
          (request ctx-atom
                   b6
                   dbz-snt ()
                   '(println 99))))
(def q6 (deref p6 200 "timeout"))
(deftest request-error-handler-6
  (is (= q6 "got exception")))

(def p7 (promise))
(defn eh7 [a e]
  (deliver p7 "got error"))
(defn exh7 [e]
  (deliver p7 "got exception"))
(def a7 (agent true :error-handler eh7))
(def b7 (agent "Fred"))
(signal a7
        (fn [agent-value ctx-atom]
          (set-exception-handler! ctx-atom exh7)
          (request ctx-atom
                   b7
                   (fn [agent-value ctx-atom] (reply ctx-atom 42)) ()
                   dbz-rsp)))
(def q7 (deref p7 200 nil))
(deftest reply-error-handler-7
  (is (= q7 "got exception")))

  (def a98 (agent 98))
  (def r98 (.getMessage (request-call a98
                                          (fn [agent-value ctx-atom]
                                            (set-exception-handler!
                                              ctx-atom
                                              (fn [e]
                                                (reply ctx-atom e)))))))
  (deftest missing-response
    (is (= r98 "Missing response")))

(def takes2 (agent nil))
(defn activate [ctx-atom] (reply ctx-atom 1024))
(defn waitforit [agent-value ctx-atom]
  (clear-ensure-response! ctx-atom)
  (set-agent-value! ctx-atom ctx-atom))
(future (Thread/sleep 1000) (send takes2 activate))
(def took2 (request-call takes2 waitforit))
(deftest coordinate
  (is (= took2 1024)))

(defn exception-result
  [agent-value ctx-atom]
  (reply ctx-atom (Exception. "exception as value")))
(def exr (.getMessage (request-call (agent nil) exception-result)))
(deftest exception-as-result
  (is (= exr "exception as value")))
