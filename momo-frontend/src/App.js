import React from "react";
import GlobalStyles from "./styles/GlobalStyles";

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
import MyReviews from "./pages/MyReviews";

import FavoriteMovies from "./pages/FavoriteMovies";
import PersonDetail from "./pages/PersonDetail";

function App() {
  return (
    <BrowserRouter>
      <GlobalStyles />
      <Routes>
        <Route element={<Layout />}>
          <Route path="/" element={<Home />} />

          <Route path="/movie/:id" element={<MovieDetail />} />

          <Route path="/search" element={<Search />} />

          {/* 전체 리뷰 */}
          <Route path="/reviews" element={<AllReviews />} />

          {/* 리뷰 상세 및 댓글 */}
          <Route path="/reviews/:reviewId" element={<ReviewDetail />} />

          {/* 마이페이지 */}
          <Route path="/profile" element={<Profile />} />

          {/* 내가 작성한 리뷰 */}
          <Route path="/profile/reviews" element={<MyReviews />} />

          {/* 회원정보 수정 및 탈퇴 */}
          <Route path="/profile/edit" element={<ProfileEdit />} />
          <Route path="/profile/delete" element={<ProfileDelete />} />

          {/* 관심 영화 */}
          <Route path="/profile/favorites" element={<FavoriteMovies />} />

          {/* 인물 상세 */}
          <Route path="/person/:type/:identifier" element={<PersonDetail />} />
        </Route>

        {/* 로그인 */}
        <Route path="/login" element={<Login />} />

        {/* 회원가입 */}
        <Route path="/signup" element={<SignUp />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
