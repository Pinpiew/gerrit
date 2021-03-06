// Copyright (C) 2012 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.gerrit.server.restapi.change;

import static com.google.gerrit.server.CommentsUtil.setCommentRevId;

import com.google.gerrit.common.TimeUtil;
import com.google.gerrit.extensions.common.CommentInfo;
import com.google.gerrit.extensions.common.Input;
import com.google.gerrit.extensions.restapi.ResourceNotFoundException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.reviewdb.client.Comment;
import com.google.gerrit.reviewdb.client.PatchSet;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.CommentsUtil;
import com.google.gerrit.server.PatchSetUtil;
import com.google.gerrit.server.change.DraftCommentResource;
import com.google.gerrit.server.patch.PatchListCache;
import com.google.gerrit.server.patch.PatchListNotAvailableException;
import com.google.gerrit.server.update.BatchUpdate;
import com.google.gerrit.server.update.BatchUpdateOp;
import com.google.gerrit.server.update.ChangeContext;
import com.google.gerrit.server.update.RetryHelper;
import com.google.gerrit.server.update.RetryingRestModifyView;
import com.google.gerrit.server.update.UpdateException;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import java.util.Collections;
import java.util.Optional;

@Singleton
public class DeleteDraftComment
    extends RetryingRestModifyView<DraftCommentResource, Input, Response<CommentInfo>> {

  private final Provider<ReviewDb> db;
  private final CommentsUtil commentsUtil;
  private final PatchSetUtil psUtil;
  private final PatchListCache patchListCache;

  @Inject
  DeleteDraftComment(
      Provider<ReviewDb> db,
      CommentsUtil commentsUtil,
      PatchSetUtil psUtil,
      RetryHelper retryHelper,
      PatchListCache patchListCache) {
    super(retryHelper);
    this.db = db;
    this.commentsUtil = commentsUtil;
    this.psUtil = psUtil;
    this.patchListCache = patchListCache;
  }

  @Override
  protected Response<CommentInfo> applyImpl(
      BatchUpdate.Factory updateFactory, DraftCommentResource rsrc, Input input)
      throws RestApiException, UpdateException {
    try (BatchUpdate bu =
        updateFactory.create(
            db.get(), rsrc.getChange().getProject(), rsrc.getUser(), TimeUtil.nowTs())) {
      Op op = new Op(rsrc.getComment().key);
      bu.addOp(rsrc.getChange().getId(), op);
      bu.execute();
    }
    return Response.none();
  }

  private class Op implements BatchUpdateOp {
    private final Comment.Key key;

    private Op(Comment.Key key) {
      this.key = key;
    }

    @Override
    public boolean updateChange(ChangeContext ctx)
        throws ResourceNotFoundException, OrmException, PatchListNotAvailableException {
      Optional<Comment> maybeComment =
          commentsUtil.getDraft(ctx.getDb(), ctx.getNotes(), ctx.getIdentifiedUser(), key);
      if (!maybeComment.isPresent()) {
        return false; // Nothing to do.
      }
      PatchSet.Id psId = new PatchSet.Id(ctx.getChange().getId(), key.patchSetId);
      PatchSet ps = psUtil.get(ctx.getDb(), ctx.getNotes(), psId);
      if (ps == null) {
        throw new ResourceNotFoundException("patch set not found: " + psId);
      }
      Comment c = maybeComment.get();
      setCommentRevId(c, patchListCache, ctx.getChange(), ps);
      commentsUtil.deleteComments(ctx.getDb(), ctx.getUpdate(psId), Collections.singleton(c));
      ctx.dontBumpLastUpdatedOn();
      return true;
    }
  }
}
