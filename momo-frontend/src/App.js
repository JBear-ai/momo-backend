import React from "react";
import "./App.css";
import { BrowserRouter, Routes, Route } from "react-router-dom";

import Home from "./pages/Home";
import MovieDetail from "./pages/MovieDetail";
import Login from "./pages/Login";
import SignUp from "./pages/SignUp";
import Layout from "./pages/Layout";
import Profile from "./pages/Profile";
import ProfileEdit from "./pages/ProfileEdit";
import ProfileDelete from "./pages/ProfileDelete";
import Search from "./pages/Search";
import AllReviews from "./pages/AllReviews";
import ReviewDetail from "./pages/ReviewDetail";
import FavoriteMovies from "./pages/FavoriteMovies";
import PersonDetail from "./pages/PersonDetail"; // 1. PersonDetail 컴포넌트 임포트 추가

function App() {
  return (
    React.createElement(
  BrowserRouter,
  null,
  React.createElement(
      Routes,
      null,
      React.createElement(
            Route,
            {"element":React.createElement(
                    Layout,
                    null
                  )},
            React.createElement(
                    Route,
                    {"path":"/","element":React.createElement(
                              Home,
                              null
                            )}
                  ),
            React.createElement(
                    Route,
                    {"path":"/movie/:id","element":React.createElement(
                              MovieDetail,
                              null
                            )}
                  ),
            React.createElement(
                    Route,
                    {"path":"/search","element":React.createElement(
                              Search,
                              null
                            )}
                  ),
            React.createElement(
                    Route,
                    {"path":"/reviews","element":React.createElement(
                              AllReviews,
                              null
                            )}
                  ),
            React.createElement(
                    Route,
                    {"path":"/reviews/:reviewId","element":React.createElement(
                              ReviewDetail,
                              null
                            )}
                  ),
            React.createElement(
                    Route,
                    {"path":"/person/:type/:identifier","element":React.createElement(
                              PersonDetail,
                              null
                            )}
                  ),
            React.createElement(
                    Route,
                    {"path":"/profile","element":React.createElement(
                              Profile,
                              null
                            )}
                  ),
            React.createElement(
                    Route,
                    {"path":"/profile/edit","element":React.createElement(
                              ProfileEdit,
                              null
                            )}
                  ),
            React.createElement(
                    Route,
                    {"path":"/profile/delete","element":React.createElement(
                              ProfileDelete,
                              null
                            )}
                  ),
            React.createElement(
                    Route,
                    {"path":"/profile/favorites","element":React.createElement(
                              FavoriteMovies,
                              null
                            )}
                  )
          ),
      React.createElement(
            Route,
            {"path":"/login","element":React.createElement(
                    Login,
                    null
                  )}
          ),
      React.createElement(
            Route,
            {"path":"/signup","element":React.createElement(
                    SignUp,
                    null
                  )}
          )
    )
)
  );
}

export default App;
